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
    private static boolean attempted;
    public void initZygote(StartupParam param) { path = param.modulePath; }

    private static synchronized Pack settings() {
        if (attempted) return pack;
        attempted = true;
        try (ZipFile zip = new ZipFile(path);
             InputStream input = zip.getInputStream(zip.getEntry("assets/profile.json"))) {
            profile = new Profile(input);
            if (profile.matches) {
                XSharedPreferences prefs = new XSharedPreferences(Profile.PACKAGE, "active");
                pack = new Pack(prefs.getString("icons", "{}"), profile);
            }
        } catch (Exception e) { Log.w("GalaxyBarStudio", "Configuration ignorée", e); }
        return pack;
    }
    static RectF fit(Bitmap image, Rect bounds) {
        float scale = Math.min((float) bounds.width()/image.getWidth(), (float) bounds.height()/image.getHeight());
        float w = image.getWidth()*scale, h = image.getHeight()*scale;
        float x = bounds.exactCenterX()-w/2, y = bounds.exactCenterY()-h/2;
        return new RectF(x,y,x+w,y+h);
    }
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam p) {
        if (!Profile.TARGET.equals(p.packageName) || settings() == null) return;
        int installed = 0;
        for (Map.Entry<String, Bitmap[]> item : pack.icons.entrySet()) {
            if (!profile.resources.has(item.getKey())) continue;
            try {
                int id = p.res.getIdentifier(item.getKey(), "drawable", p.packageName);
                if (id == 0) continue;
                Drawable original = p.res.getDrawable(id, null);
                float density = p.res.getDisplayMetrics().density;
                float width = original.getIntrinsicWidth()/density, height = original.getIntrinsicHeight()/density;
                if (width <= 0 || height <= 0 || width > 96 || height > 96) continue;
                final Bitmap[] images = item.getValue();
                p.res.setReplacement(id, new XResources.DrawableLoader() {
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
        if (!Profile.TARGET.equals(p.packageName) || !Profile.TARGET.equals(p.processName) || settings() == null) return;
        for (String key : pack.icons.keySet()) {
            if (Profile.battery(key)) {
                Battery.install(p.classLoader, pack);
                break;
            }
        }
    }
}
