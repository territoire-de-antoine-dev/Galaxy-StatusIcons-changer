package studio.galaxy.statusbar;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.lang.reflect.Field;
import de.robv.android.xposed.*;

final class Battery {
    static void install(ClassLoader loader, Pack pack) {
        try {
            Class<?> drawable = loader.loadClass("com.android.systemui.battery.SamsungBatteryMeterDrawable");
            Field state = drawable.getField("batteryState"), tint = drawable.getField("iconTint");
            Field level = state.getType().getField("level"), charge = state.getType().getField("charging");
            Field health = state.getType().getField("batteryHealth"), save = drawable.getField("powerSaveEnabled");
            Field protection = state.getType().getField("protectionState");
            XposedBridge.hookMethod(drawable.getDeclaredMethod("draw", Canvas.class), new XC_MethodHook() {
                private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                private boolean failed;
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (failed) return;
                    try {
                        Drawable self = (Drawable)p.thisObject;
                        Object current = state.get(self);
                        if (current == null || save.getBoolean(self) || protection.getInt(current) != 0 ||
                            health.getInt(current) != android.os.BatteryManager.BATTERY_HEALTH_GOOD) return;
                        int value = level.getInt(current);
                        if (value < 0 || value > 100 || self.getBounds().isEmpty()) return;
                        Bitmap[] images = pack.icons.get("battery_" + (charge.getBoolean(current) ? "charging_" : "normal_") + (value / 10 * 10));
                        if (images == null) return;
                        int color = tint.getInt(self);
                        Bitmap image = images[Color.luminance(color) > 0.5f ? 1 : 0];
                        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                        ((Canvas)p.args[0]).drawBitmap(image, null, Icons.fit(image, self.getBounds()), paint);
                        p.setResult(null);
                    } catch (RuntimeException | IllegalAccessException e) {
                        failed = true;
                        Log.w("GalaxyBarStudio", "Rendu Samsung rétabli", e);
                    }
                }
            });
        } catch (Throwable e) { Log.w("GalaxyBarStudio", "Batterie Samsung conservée", e); }
    }
}
