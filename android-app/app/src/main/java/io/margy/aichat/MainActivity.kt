package io.margy.aichat

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader

class MainActivity : Activity() {

    companion object {
        private const val START_URL = "https://appassets.androidplatform.net/assets/index.html"
        private const val RC_CAMERA = 777
        private const val RC_FILE = 1001
        private const val APP_VER = "1.4"
    }

    private lateinit var web: WebView
    private lateinit var loader: WebViewAssetLoader
    private var pendingCam: PermissionRequest? = null
    private var pendingFile: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyStatusBar()

        web = WebView(this)
        setContentView(web)

        loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = false
            textZoom = 100
            setSupportMultipleWindows(true)
        }

        // фокус и клавиатура
        web.isFocusable = true
        web.isFocusableInTouchMode = true
        web.isClickable = true
        web.isLongClickable = true
        web.requestFocus(View.FOCUS_DOWN)
        web.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> if (!v.hasFocus()) v.requestFocusFromTouch()
            }
            false
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return loader.shouldInterceptRequest(request.url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.host == "appassets.androidplatform.net") return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, u))
                } catch (_: Exception) {}
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                injectDark()
                web.requestFocus(View.FOCUS_DOWN)
            }
        }

        web.webChromeClient = object : WebChromeClient() {

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val needCam = request.resources.any { it == PermissionRequest.RESOURCE_VIDEO_CAPTURE }
                    if (!needCam) {
                        request.deny()
                        return@runOnUiThread
                    }
                    if (checkSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED) {
                        request.grant(request.resources)
                    } else {
                        pendingCam = request
                        requestPermissions(arrayOf("android.permission.CAMERA"), RC_CAMERA)
                    }
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                val t = params.acceptTypes
                var type = if (!t.isNullOrEmpty() && !t[0]?.trim().isNullOrEmpty()) t[0] else "*/*"
                if (type.contains(",")) type = "*/*"
                i.type = type
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                return try {
                    startActivityForResult(Intent.createChooser(i, "Выбрать файл"), RC_FILE)
                    pendingFile = callback
                    true
                } catch (e: Exception) {
                    pendingFile = null
                    false
                }
            }

            override fun onCreateWindow(
                view: WebView,
                dialog: Boolean,
                userGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                try {
                    val tmp = WebView(view.context)
                    tmp.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                            val u = r.url
                            if (u.host != "appassets.androidplatform.net") {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, u))
                                } catch (_: Exception) {}
                            }
                            return true
                        }
                    }
                    (resultMsg.obj as WebView.WebViewTransport).setWebView(tmp)
                } catch (_: Exception) {}
                return true
            }
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(START_URL)

        // сброс кэша WebView при обновлении версии
        val sp: SharedPreferences = getPreferences(MODE_PRIVATE)
        if (sp.getString("ver", "") != APP_VER) {
            web.clearCache(true)
            sp.edit().putString("ver", APP_VER).apply()
        }
    }

    private fun isDark(): Boolean {
        val m = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return m == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyStatusBar() {
        val w: Window = window ?: return
        w.statusBarColor = if (isDark()) 0xff141218.toInt() else 0xfffef7ff.toInt()
        val d = w.decorView
        var f = d.systemUiVisibility
        f = if (isDark()) f and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        else f or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        d.systemUiVisibility = f
    }

    private fun injectDark() {
        web.evaluateJavascript(
            "window.__MARGY_DARK=" + isDark() + ";if(window.__applyTheme)window.__applyTheme();",
            null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_FILE) {
            pendingFile?.let {
                it.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
                pendingFile = null
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == RC_CAMERA && pendingCam != null) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingCam?.grant(pendingCam?.resources)
            } else {
                pendingCam?.deny()
                Toast.makeText(this, "Камера запрещена", Toast.LENGTH_SHORT).show()
            }
            pendingCam = null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyStatusBar()
        injectDark()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
