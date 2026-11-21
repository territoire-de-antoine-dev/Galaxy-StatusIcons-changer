package studio.galaxy.statusbar;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBinder;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PublishedPackTest {
    @Test public void onlyStudioAndSystemUiCanReadTheLatestPublishedFile() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        PublishedPack provider = Robolectric.setupContentProvider(PublishedPack.class);
        PackageInfo systemUi = new PackageInfo();
        systemUi.packageName = Profile.TARGET;
        systemUi.applicationInfo = new ApplicationInfo();
        systemUi.applicationInfo.packageName = Profile.TARGET;
        systemUi.applicationInfo.uid = 10699;
        Shadows.shadowOf(ctx.getPackageManager()).installPackage(systemUi);
        String snapshot = "{\"stat_sys_wifi_signal_4\":{}}";
        PublishedPack.save(ctx, snapshot);
        try {
            ShadowBinder.setCallingUid(10699);
            assertEquals(snapshot, read(provider));
            ShadowBinder.setCallingUid(10422);
            assertThrows(SecurityException.class, () -> read(provider));
            ShadowBinder.setCallingUid(Process.myUid());
            PublishedPack.save(ctx, "{}");
            assertEquals("{}", read(provider));
            assertThrows(java.io.FileNotFoundException.class, () -> provider.openFile(PublishedPack.URI,"rw"));
        } finally { ShadowBinder.setCallingUid(Process.myUid()); }
    }

    private String read(PublishedPack provider) throws Exception {
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(provider.openFile(PublishedPack.URI,"r"))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
