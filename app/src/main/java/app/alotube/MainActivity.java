package app.alotube;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Rational;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    static final String ORIGIN = "https://appassets.androidplatform.net";
    static final String UA = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";
    static final int REQ_FILE = 1;

    WebView web, yt;
    FrameLayout root;
    LinearLayout ytBox;
    WebViewAssetLoader loader;
    ValueCallback<Uri[]> chooser;
    View customView;
    WebChromeClient.CustomViewCallback customCb;

    final AtomicInteger nextId = new AtomicInteger(1);
    final Map<Integer, OutputStream> outs = new ConcurrentHashMap<>();
    final Map<Integer, Uri> uris = new ConcurrentHashMap<>();
    final Map<Integer, File> files = new ConcurrentHashMap<>();
    final Map<Integer, String> names = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF0E1512);
        getWindow().setNavigationBarColor(0xFF0E1512);

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0E1512);
        setContentView(root);

        loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        setup(web, true);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        ytBox = new LinearLayout(this);
        ytBox.setOrientation(LinearLayout.VERTICAL);
        ytBox.setBackgroundColor(0xFF0E1512);
        ytBox.setVisibility(View.GONE);
        Button close = new Button(this);
        close.setText("\u2715  Volver a AloTube");
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeYt();
            }
        });
        ytBox.addView(close, new LinearLayout.LayoutParams(-1, -2));
        yt = new WebView(this);
        setup(yt, false);
        ytBox.addView(yt, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(ytBox, new FrameLayout.LayoutParams(-1, -1));

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 2);
        }

        web.loadUrl(ORIGIN + "/assets/alotube.html");
    }

    void setup(WebView w, final boolean main) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(w, true);

        w.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (!main) return null;
                return intercept(r);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (main && !r.getUrl().toString().startsWith(ORIGIN)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
                    } catch (Exception e) {
                        // sin aplicación para abrir el enlace
                    }
                    return true;
                }
                return false;
            }
        });

        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                try {
                    startActivityForResult(p.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    chooser = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (customView != null) {
                    cb.onCustomViewHidden();
                    return;
                }
                customView = view;
                customCb = cb;
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }

            @Override
            public void onHideCustomView() {
                hideCustom();
            }
        });

        if (main) w.addJavascriptInterface(new Bridge(), "AloAndroid");
    }

    void hideCustom() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        if (customCb != null) customCb.onCustomViewHidden();
        customCb = null;
    }

    void closeYt() {
        ytBox.setVisibility(View.GONE);
        yt.loadUrl("about:blank");
    }

    static String header(WebResourceRequest r, String k) {
        for (Map.Entry<String, String> e : r.getRequestHeaders().entrySet()) {
            if (e.getKey().equalsIgnoreCase(k)) return e.getValue();
        }
        return null;
    }

    /** Permite que la p\u00e1gina haga fetch a cualquier sitio (GET/HEAD) sin depender de proxies p\u00fablicos. */
    WebResourceResponse intercept(WebResourceRequest r) {
        Uri u = r.getUrl();
        WebResourceResponse a = loader.shouldInterceptRequest(u);
        if (a != null) return a;

        String origin = header(r, "Origin");
        if (origin == null || !origin.equals(ORIGIN)) return null;
        String host = u.getHost() == null ? "" : u.getHost();
        if (host.endsWith("google.com") || host.endsWith("googleapis.com") || host.endsWith("gstatic.com")) return null;

        String m = r.getMethod();
        Map<String, String> cors = new HashMap<>();
        cors.put("Access-Control-Allow-Origin", ORIGIN);
        cors.put("Access-Control-Allow-Headers", "Range, Content-Type, Accept");
        cors.put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        cors.put("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges, Content-Type");

        if ("OPTIONS".equals(m)) {
            return new WebResourceResponse("text/plain", "utf-8", 204, "No Content", cors,
                    new ByteArrayInputStream(new byte[0]));
        }
        if (!"GET".equals(m) && !"HEAD".equals(m)) return null;

        try {
            String cur = u.toString();
            for (int i = 0; i < 6; i++) {
                HttpURLConnection c = (HttpURLConnection) new URL(cur).openConnection();
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setRequestMethod(m);
                c.setRequestProperty("User-Agent", UA);
                c.setRequestProperty("Accept-Encoding", "identity");
                String[] pass = {"Range", "Accept", "Accept-Language"};
                for (String k : pass) {
                    String val = header(r, k);
                    if (val != null) c.setRequestProperty(k, val);
                }
                int code = c.getResponseCode();
                String loc = c.getHeaderField("Location");
                if (code >= 300 && code < 400 && loc != null) {
                    cur = new URL(new URL(cur), loc).toString();
                    c.disconnect();
                    continue;
                }

                Map<String, String> h = new HashMap<>(cors);
                for (Map.Entry<String, List<String>> e : c.getHeaderFields().entrySet()) {
                    String k = e.getKey();
                    if (k == null) continue;
                    String lk = k.toLowerCase();
                    if (lk.startsWith("access-control-") || lk.equals("transfer-encoding")
                            || lk.equals("connection") || lk.equals("content-type")) continue;
                    StringBuilder sb = new StringBuilder();
                    for (String x : e.getValue()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(x);
                    }
                    h.put(k, sb.toString());
                }

                String mime = "application/octet-stream";
                String enc = null;
                String ct = c.getContentType();
                if (ct != null) {
                    String[] parts = ct.split(";");
                    mime = parts[0].trim();
                    for (String p : parts) {
                        String t = p.trim();
                        if (t.toLowerCase().startsWith("charset=")) enc = t.substring(8);
                    }
                }
                InputStream in = null;
                if (!"HEAD".equals(m)) in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                if (in == null) in = new ByteArrayInputStream(new byte[0]);
                String reason = c.getResponseMessage();
                if (reason == null || reason.isEmpty()) reason = "OK";
                return new WebResourceResponse(mime, enc, code, reason, h, in);
            }
        } catch (Exception e) {
            // si falla, la p\u00e1gina usar\u00e1 sus proxies de respaldo
        }
        return null;
    }

    class Bridge {
        @JavascriptInterface
        public int open(String name, String mime) {
            try {
                String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");
                int id = nextId.getAndIncrement();
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DISPLAY_NAME, safe);
                    v.put(MediaStore.MediaColumns.MIME_TYPE,
                            (mime == null || mime.isEmpty()) ? "application/octet-stream" : mime);
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AloTube");
                    v.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) return -1;
                    OutputStream o = getContentResolver().openOutputStream(uri);
                    if (o == null) return -1;
                    outs.put(id, o);
                    uris.put(id, uri);
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (dir == null) return -1;
                    File f = new File(dir, safe);
                    outs.put(id, new FileOutputStream(f));
                    files.put(id, f);
                }
                names.put(id, safe);
                return id;
            } catch (Exception e) {
                return -1;
            }
        }

        @JavascriptInterface
        public boolean write(int id, String b64) {
            OutputStream o = outs.get(id);
            if (o == null) return false;
            try {
                o.write(Base64.decode(b64, Base64.DEFAULT));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean close(int id) {
            OutputStream o = outs.remove(id);
            if (o == null) return false;
            try {
                o.flush();
                o.close();
                Uri uri = uris.remove(id);
                if (uri != null) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    getContentResolver().update(uri, v, null, null);
                }
                files.remove(id);
                final String n = names.remove(id);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Guardado: " + n + " (Descargas/AloTube)", Toast.LENGTH_LONG).show();
                    }
                });
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void abort(int id) {
            OutputStream o = outs.remove(id);
            try {
                if (o != null) o.close();
            } catch (Exception e) {
                // ignorar
            }
            Uri uri = uris.remove(id);
            if (uri != null) {
                try {
                    getContentResolver().delete(uri, null, null);
                } catch (Exception e) {
                    // ignorar
                }
            }
            File f = files.remove(id);
            if (f != null) f.delete();
            names.remove(id);
        }

        @JavascriptInterface
        public void pip() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        PictureInPictureParams p = new PictureInPictureParams.Builder()
                                .setAspectRatio(new Rational(16, 9)).build();
                        enterPictureInPictureMode(p);
                    } catch (Exception e) {
                        // el dispositivo no permite ventana flotante
                    }
                }
            });
        }

        @JavascriptInterface
        public void audioStart(String title) {
            try {
                Intent i = new Intent(MainActivity.this, PlaybackService.class);
                i.putExtra("title", title);
                startForegroundService(i);
            } catch (Exception e) {
                // sin permiso para iniciar el servicio
            }
        }

        @JavascriptInterface
        public void audioStop() {
            try {
                stopService(new Intent(MainActivity.this, PlaybackService.class));
            } catch (Exception e) {
                // ignorar
            }
        }

        @JavascriptInterface
        public void openYouTube() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ytBox.setVisibility(View.VISIBLE);
                    String cur = yt.getUrl();
                    if (cur == null || cur.startsWith("about:")) yt.loadUrl("https://m.youtube.com/");
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_FILE && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            chooser = null;
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean inPip, Configuration cfg) {
        super.onPictureInPictureModeChanged(inPip, cfg);
        web.evaluateJavascript("window.__pip&&window.__pip(" + inPip + ")", null);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            hideCustom();
            return;
        }
        if (ytBox.getVisibility() == View.VISIBLE) {
            if (yt.canGoBack()) yt.goBack();
            else closeYt();
            return;
        }
        if (web.canGoBack()) {
            web.goBack();
            return;
        }
        super.onBackPressed();
    }
}
