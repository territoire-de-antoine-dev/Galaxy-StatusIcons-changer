package studio.galaxy.statusbar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class Pack {
    static final int MAX_TEXT = 2 * 1024 * 1024;
    final Map<String, Bitmap[]> icons = new HashMap<>();

    final boolean pixelBattery, chargingWave, satellite;
    Pack(String text, Profile profile) throws Exception {
        if (text.length() > MAX_TEXT) throw new IllegalArgumentException("Pack supérieur à 2 Mio");
        JSONObject root = new JSONObject(text);
        JSONObject features = root.optJSONObject("_features");
        pixelBattery = features != null && features.optBoolean("pixelBattery");
        chargingWave = features != null && features.optBoolean("chargingWave");
        satellite = features != null && features.optBoolean("satellite");
        root.remove("_features");
        if (root.length() > 384) throw new IllegalArgumentException("Trop de ressources");
        Map<String,Bitmap> decoded = new HashMap<>();
        Iterator<String> names = root.keys();
        while (names.hasNext()) {
            String key = names.next();
            if (!profile.allowed(key)) throw new IllegalArgumentException("Cible inconnue");
            JSONObject variants = root.getJSONObject(key);
            Bitmap light = decoded.computeIfAbsent(variants.getString("day"), Pack::decode);
            Bitmap dark = variants.has("night") ? decoded.computeIfAbsent(variants.getString("night"), Pack::decode) : light;
            icons.put(key, new Bitmap[]{light, dark});
        }
    }
    private static Bitmap decode(String encoded) {
        if (encoded.length() > 100000) throw new IllegalArgumentException("Image trop grande");
        byte[] data = Base64.decode(encoded, Base64.DEFAULT);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        if (!"image/png".equals(opts.outMimeType) || opts.outWidth < 1 || opts.outHeight < 1 ||
            opts.outWidth > 128 || opts.outHeight > 128) throw new IllegalArgumentException("PNG invalide");
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) throw new IllegalArgumentException("PNG illisible");
        return bitmap;
    }
}
