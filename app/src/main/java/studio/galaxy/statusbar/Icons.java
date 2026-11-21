package studio.galaxy.statusbar;

import android.content.res.Configuration;
import android.content.res.XResources;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipFile;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Icons implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    private static String path;
    private static Pack pack;
    private static Profile profile;
    private static final java.util.WeakHashMap<XResources, Boolean> resources = new java.util.WeakHashMap<>();
    public void initZygote(StartupParam param) { path = param.modulePath; }

    private static void load(android.content.Context context, ClassLoader loader) {
        try (ZipFile zip = new ZipFile(path);
             InputStream input = zip.getInputStream(zip.getEntry("assets/profile.json"))) {
            Profile checked = new Profile(input);
            if (!checked.matches) return;
            try (InputStream stream = context.getContentResolver().openInputStream(PublishedPack.URI)) {
                if (stream == null) throw new java.io.IOException("Fournisseur indisponible");
                java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
                byte[] block = new byte[8192];
                int count;
                while ((count = stream.read(block)) != -1) {
                    if (data.size() + count > Pack.MAX_TEXT) throw new java.io.IOException("Pack trop grand");
                    data.write(block,0,count);
                }
                Pack active = new Pack(data.toString("UTF-8"), checked);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> activate(checked,active,loader,context));
            }
        } catch (Exception e) { Log.w("GalaxyBarStudio", "Lecture du pack publié impossible", e); }
    }
    private static synchronized void activate(Profile checked, Pack active, ClassLoader loader, android.content.Context context) {
        profile = checked;
        pack = active;
        for (XResources res : new java.util.ArrayList<>(resources.keySet())) install(res);
        if (pack.pixelBattery || pack.icons.keySet().stream().anyMatch(Profile::battery)) Battery.install(loader,pack);
        if (pack.satellite) MobileSatellite.install(loader);
        if (pack.chargingWave) ChargingWave.install(context);
        Log.i("GalaxyBarStudio","États lus depuis le Studio " + active.icons.size());
    }
    static RectF fit(Bitmap image, Rect bounds) {
        float scale = Math.min((float) bounds.width()/image.getWidth(), (float) bounds.height()/image.getHeight());
        float w = image.getWidth()*scale, h = image.getHeight()*scale;
        float x = bounds.exactCenterX()-w/2, y = bounds.exactCenterY()-h/2;
        return new RectF(x,y,x+w,y+h);
    }
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam p) {
        if (!Profile.TARGET.equals(p.packageName)) return;
        synchronized (Icons.class) {
            if (resources.put(p.res, Boolean.TRUE) == null && pack != null) install(p.res);
        }
    }
    private static void install(XResources res) {
        int installed = 0;
        for (Map.Entry<String, Bitmap[]> item : pack.icons.entrySet()) {
            if (!profile.resources.has(item.getKey())) continue;
            try {
                int id = res.getIdentifier(item.getKey(), "drawable", Profile.TARGET);
                if (id == 0) continue;
                Drawable original = res.getDrawable(id, null);
                float density = res.getDisplayMetrics().density;
                float width = original.getIntrinsicWidth()/density, height = original.getIntrinsicHeight()/density;
                if (width <= 0 || height <= 0 || width > 96 || height > 96) continue;
                final Bitmap[] images = item.getValue();
                res.setReplacement(id, new XResources.DrawableLoader() {
                    public Drawable newDrawable(XResources res, int ignored) {
                        boolean night = (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                        Bitmap source = images[night ? 1 : 0];
                        float d = res.getDisplayMetrics().density;
                        int w = Math.max(1, Math.round(width*d)), h = Math.max(1, Math.round(height*d));
                        Bitmap output = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                        output.setDensity(res.getDisplayMetrics().densityDpi);
                        new Canvas(output).drawBitmap(source,null,fit(source,new Rect(0,0,w,h)),new Paint(Paint.FILTER_BITMAP_FLAG));
                        return new BitmapDrawable(res,output);
                    }
                });
                installed++;
            } catch (RuntimeException e) { Log.w("GalaxyBarStudio", "Cible ignorée " + item.getKey()); }
        }
        Log.i("GalaxyBarStudio", "Ressources enregistrées " + installed);
    }
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (!Profile.TARGET.equals(p.packageName) || !Profile.TARGET.equals(p.processName)) return;
        XposedHelpers.findAndHookMethod(android.app.Application.class, "attach", android.content.Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                android.content.Context ctx = (android.content.Context)param.args[0];
                if (Profile.TARGET.equals(ctx.getPackageName())) {
                    new Thread(() -> load(ctx,p.classLoader), "GalaxyStudio-pack").start();
                }
            }
        });
    }
}
