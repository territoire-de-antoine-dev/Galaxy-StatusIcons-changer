package studio.galaxy.statusbar;

import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class PublishedPack extends ContentProvider {
    static final Uri URI = Uri.parse("content://studio.galaxy.statusbar.packs/active");
    private static File target(Context ctx) {
        return new File(ctx.createDeviceProtectedStorageContext().getFilesDir(), "published-icons.json");
    }
    static synchronized void save(Context ctx, String json) throws IOException {
        AtomicFile file = new AtomicFile(target(ctx));
        FileOutputStream out = file.startWrite();
        try {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            file.finishWrite(out);
        } catch (IOException | RuntimeException failure) {
            file.failWrite(out);
            throw failure;
        }
    }
    @Override public boolean onCreate() { return true; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!URI.equals(uri) || !"r".equals(mode)) throw new FileNotFoundException("Lecture seule");
        Context ctx = getContext();
        int caller = Binder.getCallingUid();
        if (caller != android.os.Process.myUid()) {
            try {
                if (caller != ctx.getPackageManager().getPackageUid(Profile.TARGET, 0))
                    throw new SecurityException("Lecture réservée à SystemUI");
            } catch (PackageManager.NameNotFoundException e) { throw new SecurityException(e); }
        }
        synchronized (PublishedPack.class) {
            return ParcelFileDescriptor.open(target(ctx), ParcelFileDescriptor.MODE_READ_ONLY);
        }
    }
    @Override public String getType(Uri uri) { return URI.equals(uri) ? "application/json" : null; }
    @Override public Cursor query(Uri uri,String[] fields,String where,String[] args,String order) { throw new UnsupportedOperationException(); }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String where,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String where,String[] args) { throw new UnsupportedOperationException(); }
}
