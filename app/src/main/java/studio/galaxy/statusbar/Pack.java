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

    Pack(String text, Profile profile) throws Exception {
        if (text.length() > MAX_TEXT) throw new IllegalArgumentException("Pack supérieur à 2 Mio");
        JSONObject root = new JSONObject(text);
        if (root.length() > 256) throw new IllegalArgumentException("Trop de ressources");
        Iterator<String> names = root.keys();
        while (names.hasNext()) {
            String key = names.next();
            if (!profile.allowed(key)) throw new IllegalArgumentException("Cible inconnue");
            JSONObject variants = root.getJSONObject(key);
            Bitmap light = decode(variants.getString("day"));
            Bitmap dark = variants.has("night") ? decode(variants.getString("night")) : light;
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
