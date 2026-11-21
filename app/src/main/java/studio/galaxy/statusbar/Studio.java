package studio.galaxy.statusbar;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class Studio extends Activity {
    private static final String ORIGIN = "https://studio.invalid/";
    private ValueCallback<Uri[]> selection;
    private WebView web;
    private Profile profile;
    private String export;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try (InputStream in = getAssets().open("profile.json")) { profile = new Profile(in); }
        catch (Exception error) { throw new IllegalStateException(error); }
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith(ORIGIN)) {
                    String asset = url.substring(ORIGIN.length());
                    String mime = asset.equals("index.html") ? "text/html" : asset.equals("editor.js") ? "application/javascript" :
                                  asset.equals("style.css") ? "text/css" : asset.equals("profile.json") ? "application/json" : null;
                    if (mime != null) try { return new WebResourceResponse(mime,"UTF-8",getAssets().open(asset)); }
                    catch (IOException ignored) { }
                }
                return new WebResourceResponse("text/plain","UTF-8",null);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (selection != null) selection.onReceiveValue(null);
                selection = cb;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE);
                String[] types = params.getAcceptTypes();
                intent.setType("*/*");
                if (types.length > 0) intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
                startActivityForResult(intent,1);
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "Studio");
        setContentView(web);
        web.loadUrl(ORIGIN + "index.html");
    }
    final class Bridge {
        @JavascriptInterface public String publish(String text) {
            try {
                if (!text.equals("{}") && !profile.matches) return "Firmware différent du profil analysé.";
                new Pack(text,profile);
                boolean saved = getSharedPreferences("active", MODE_WORLD_READABLE).edit().putString("icons",text).commit();
                return saved ? "Enregistré pour le prochain chargement de SystemUI." : "Échec de l’écriture.";
            } catch (SecurityException e) { return "Partage LSPosed indisponible. Le brouillon reste enregistré."; }
            catch (Exception e) { return "Pack refusé — " + e.getMessage(); }
        }
        @JavascriptInterface public void backup(String json) {
            if (json.length() > Pack.MAX_TEXT * 2) return;
            runOnUiThread(() -> {
                export = json;
                startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json").putExtra(Intent.EXTRA_TITLE,"galaxy-pack.json"),2);
            });
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        Uri uri = result == RESULT_OK && data != null ? data.getData() : null;
        if (request == 1 && selection != null) {
            selection.onReceiveValue(uri == null || !"content".equals(uri.getScheme()) ? null : new Uri[]{uri});
            selection = null;
        }
        if (request == 2 && uri != null && export != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri,"wt")) {
                if (out == null) throw new IOException();
                out.write(export.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this,"Pack exporté",Toast.LENGTH_SHORT).show();
            } catch (IOException e) { Toast.makeText(this,"Échec de l’export",Toast.LENGTH_LONG).show(); }
        }
        if (request == 2) export = null;
    }
}
