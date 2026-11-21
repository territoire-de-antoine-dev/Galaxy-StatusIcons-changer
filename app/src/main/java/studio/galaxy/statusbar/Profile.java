package studio.galaxy.statusbar;

import android.os.Build;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class Profile {
    static final String PACKAGE = "studio.galaxy.statusbar", TARGET = "com.android.systemui";
    static final String BATTERY_BUILD = "CP2A.260605.016.S911U1UEU8GZIF";
    final JSONObject resources;
    final boolean matches;
    Profile(InputStream input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] part = new byte[4096];
        int count;
        while ((count = input.read(part)) != -1) buffer.write(part, 0, count);
        JSONObject config = new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));
        resources = config.getJSONObject("resources");
        matches = Build.MODEL.equals(config.getString("model")) && Build.DISPLAY.equals(config.getString("build"));
    }
    static boolean battery(String key) {
        return Build.DISPLAY.equals(BATTERY_BUILD) && key.matches("battery_(normal|charging)_(100|[1-9]0|0)");
    }
    boolean allowed(String name) { return resources.has(name) || battery(name); }
}
