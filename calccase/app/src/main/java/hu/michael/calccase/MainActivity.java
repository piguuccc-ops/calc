package hu.michael.calccase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A single full-screen WebView showing the calculator rig, which lives in
 * app/src/main/assets.
 *
 * The page is served from https://calcase.local/ rather than
 * file:///android_asset/. A file:// page gets an opaque origin in WebView,
 * where localStorage is unreliable and IndexedDB is refused outright — and
 * the rig keeps its calibration in the first and its note images in the
 * second. Serving the same bytes under a real https origin makes both work
 * exactly as they do in a browser.
 */
public class MainActivity extends Activity {

    private static final String ORIGIN = "https://calcase.local/";
    private WebView web;
    private long lastBack = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);                       // ignore the system font-size setting
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(false);
        web.setLongClickable(false);
        web.setOnLongClickListener(v -> true);    // no text-selection popup on a key press

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (!url.startsWith(ORIGIN)) return null;
                String path = url.substring(ORIGIN.length());
                int q = path.indexOf('?');
                if (q >= 0) path = path.substring(0, q);
                if (path.isEmpty()) path = "index.html";
                try {
                    InputStream in = getAssets().open(path);
                    return new WebResourceResponse(mime(path), "UTF-8", in);
                } catch (IOException e) {
                    return new WebResourceResponse("text/plain", "UTF-8", 404,
                        "Not Found", new java.util.HashMap<>(), null);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                // the rig is self-contained; never let anything navigate away
                return !req.getUrl().toString().startsWith(ORIGIN);
            }
        });

        setContentView(web);
        web.loadUrl(ORIGIN + "index.html");

        // Keep Android's back-swipe off the outer key columns. The platform caps
        // exclusions at 200dp of height per edge, so this protects the bottom rows
        // — the ones a thumb reaches for — and immersive mode covers the rest.
        web.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> applyExclusions());
    }

    private void applyExclusions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        int w = web.getWidth(), h = web.getHeight();
        if (w == 0 || h == 0) return;
        int strip = Math.round(28 * getResources().getDisplayMetrics().density);
        int band = Math.round(200 * getResources().getDisplayMetrics().density);
        List<Rect> rects = new ArrayList<>();
        rects.add(new Rect(0, h - band, strip, h));
        rects.add(new Rect(w - strip, h - band, w, h));
        web.setSystemGestureExclusionRects(rects);
    }

    private static String mime(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".js")) return "text/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    private void goImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            web.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_FULLSCREEN
              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) goImmersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        goImmersive();
    }

    /** Back is a key press away from losing your calibration, so make it deliberate. */
    @Override
    public void onBackPressed() {
        long now = System.currentTimeMillis();
        if (now - lastBack < 2000) {
            super.onBackPressed();
        } else {
            lastBack = now;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
    }
}
