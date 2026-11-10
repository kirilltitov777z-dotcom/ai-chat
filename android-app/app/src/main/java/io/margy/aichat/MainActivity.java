package io.margy.aichat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {

    private static final String START_URL = "https://appassets.androidplatform.net/assets/index.html";
    private static final int RC_CAMERA = 777;
    private static final int RC_FILE = 1001;

    private WebView web;
    private WebViewAssetLoader loader;
    private PermissionRequest pendingCam;
    private ValueCallback<Uri[]> pendingFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyStatusBar();

        web = new WebView(this);
        setContentView(web);

        loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings st = web.getSettings();
        st.setJavaScriptEnabled(true);
        st.setDomStorageEnabled(true);
        st.setDatabaseEnabled(true);
        st.setAllowFileAccess(false);
        st.setMediaPlaybackRequiresUserGesture(false);
        st.setTextZoom(100);
        st.setSupportMultipleWindows(true);

        // фокус и клавиатура
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.setClickable(true);
        web.setLongClickable(true);
        web.requestFocus(View.FOCUS_DOWN);
        web.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) v.requestFocusFromTouch();
                        break;
                }
                return false;
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                injectDark();
                web.requestFocus(View.FOCUS_DOWN);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boolean needCam = false;
                        for (String r : request.getResources()) {
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) needCam = true;
                        }
                        if (!needCam) {
                            request.deny();
                            return;
                        }
                        if (checkSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED) {
                            request.grant(request.getResources());
                        } else {
                            pendingCam = request;
                            requestPermissions(new String[]{"android.permission.CAMERA"}, RC_CAMERA);
                        }
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                String[] t = p.getAcceptTypes();
                String type = (t != null && t.length > 0 && t[0] != null && t[0].trim().length() > 0) ? t[0] : "*/*";
                if (type.contains(",")) type = "*/*";
                i.setType(type);
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(Intent.createChooser(i, "Выбрать файл"), RC_FILE);
                    pendingFile = cb;
                    return true;
                } catch (Exception e) {
                    pendingFile = null;
                    return false;
                }
            }

            @Override
            public boolean onCreateWindow(WebView v, boolean dialog, boolean userGesture, Message resultMsg) {
                try {
                    WebView tmp = new WebView(v.getContext());
                    tmp.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView vv, WebResourceRequest r) {
                            Uri u = r.getUrl();
                            if (!"appassets.androidplatform.net".equals(u.getHost())) {
                                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                            }
                            return true;
                        }
                    });
                    ((WebView.WebViewTransport) resultMsg.obj).setWebView(tmp);
                } catch (Exception ignored) {}
                return true;
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(START_URL);
        }

        // сброс кэша при обновлении версии, чтобы страница не осталась старой
        android.content.SharedPreferences sp = getPreferences(MODE_PRIVATE);
        if (!"1.3".equals(sp.getString("ver", ""))) {
            web.clearCache(true);
            sp.edit().putString("ver", "1.3").apply();
        }
    }

    private boolean isDark() {
        int m = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return m == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyStatusBar() {
        Window w = getWindow();
        if (w == null) return;
        w.setStatusBarColor(isDark() ? 0xff141218 : 0xfffef7ff);
        View d = w.getDecorView();
        int f = d.getSystemUiVisibility();
        if (isDark()) f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        else f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        d.setSystemUiVisibility(f);
    }

    private void injectDark() {
        if (web == null) return;
        web.evaluateJavascript(
                "window.__MARGY_DARK=" + isDark() + ";if(window.__applyTheme)window.__applyTheme();",
                null);
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        if (rc == RC_FILE) {
            if (pendingFile != null) {
                pendingFile.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
                pendingFile = null;
            }
        } else {
            super.onActivityResult(rc, res, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] ps, @NonNull int[] gr) {
        if (rc == RC_CAMERA && pendingCam != null) {
            if (gr != null && gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) {
                pendingCam.grant(pendingCam.getResources());
            } else {
                pendingCam.deny();
                Toast.makeText(this, "Камера запрещена", Toast.LENGTH_SHORT).show();
            }
            pendingCam = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applyStatusBar();
        injectDark();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
