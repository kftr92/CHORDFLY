package com.example.youtube

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "CHORDFLY_YOUTUBE"
private const val REFERER_HEADER = "https://com.aistudio.chordify.app/"
private const val ORIGIN_PARAM = "https://com.aistudio.chordify.app"

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

class FullscreenWebChromeClient(
    private val activity: Activity?,
    private val onFullscreenChanged: (Boolean) -> Unit
) : WebChromeClient() {
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            onHideCustomView()
            return
        }

        customView = view
        customViewCallback = callback

        activity?.let { act ->
            val contentContainer = act.findViewById<ViewGroup>(android.R.id.content)
                ?: (act.window.decorView as? ViewGroup)
            contentContainer?.addView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        onFullscreenChanged(true)
    }

    override fun onHideCustomView() {
        if (customView == null) return

        activity?.let { act ->
            val contentContainer = act.findViewById<ViewGroup>(android.R.id.content)
                ?: (act.window.decorView as? ViewGroup)
            contentContainer?.removeView(customView)
        }

        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null

        onFullscreenChanged(false)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebPlayer(
    targetUrl: String,
    onTimeUpdate: (Float) -> Unit,
    onStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var isFullscreen by remember { mutableStateOf(false) }

    val currentOnTimeUpdate by rememberUpdatedState(onTimeUpdate)
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val chromeClient = remember(activity) {
        FullscreenWebChromeClient(activity) { inFullscreen ->
            isFullscreen = inFullscreen
        }
    }

    BackHandler(enabled = isFullscreen) {
        chromeClient.onHideCustomView()
    }

    val videoId = remember(targetUrl) { extractYouTubeId(targetUrl) }
    val embedUrl = remember(videoId) {
        "https://www.youtube.com/embed/$videoId?playsinline=1&controls=1&rel=0&enablejsapi=1&origin=$ORIGIN_PARAM"
    }

    class AndroidJsBridge {
        private var lastLoggedTime = -1f

        @JavascriptInterface
        fun onTimeUpdate(seconds: Float) {
            if (!seconds.isFinite() || seconds < 0f) return
            if (Math.abs(seconds - lastLoggedTime) >= 1.0f) {
                lastLoggedTime = seconds
                Log.d(TAG, "PLAYER_TIME=%.1f".format(seconds))
            }
            mainHandler.post {
                currentOnTimeUpdate(seconds)
            }
        }

        @JavascriptInterface
        fun onStateChange(playing: Boolean, stateName: String?) {
            val name = stateName ?: if (playing) "PLAYING" else "PAUSED"
            Log.d(TAG, "PLAYER_STATE=$name")
            mainHandler.post {
                currentOnStateChange(playing)
            }
        }

        @JavascriptInterface
        fun log(message: String) {
            Log.d(TAG, message)
        }
    }

    fun injectSyncScript(webView: WebView) {
        val js = """
            (function() {
                if (window.__chordflyTimeSync) return;
                window.__chordflyTimeSync = true;

                var lastTime = -1;
                var lastState = null;

                function update() {
                    try {
                        var video = document.querySelector('video');
                        if (!video) return;

                        var time = Number(video.currentTime);
                        if (!isFinite(time) || time < 0) return;

                        if (Math.abs(time - lastTime) >= 0.05) {
                            lastTime = time;
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onTimeUpdate(time);
                            }
                        }

                        var state;
                        if (video.ended) {
                            state = "ENDED";
                        } else if (video.paused) {
                            state = "PAUSED";
                        } else {
                            state = "PLAYING";
                        }

                        if (state !== lastState) {
                            lastState = state;
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onStateChange(state === "PLAYING", state);
                            }
                        }
                    } catch(e) {}
                }

                window.__chordflySyncTimer = setInterval(update, 150);
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.BLACK)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webChromeClient = chromeClient
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d(TAG, "PAGE_STARTED=$url")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "PAGE_FINISHED=$url")
                        view?.let { webView ->
                            Log.d(TAG, "WEBVIEW_SIZE=${webView.width}x${webView.height}")
                            Log.d(TAG, "WEBVIEW_VISIBLE=${webView.visibility == View.VISIBLE}")
                            Log.d(TAG, "WEBVIEW_ALPHA=${webView.alpha}")
                            Log.d(TAG, "WEBVIEW_SCALE=${webView.scaleX}")

                            injectSyncScript(webView)
                        }
                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        Log.d(TAG, "PAGE_COMMIT_VISIBLE=$url")
                        view?.let { webView ->
                            injectSyncScript(webView)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        Log.e(TAG, "WEB_RESOURCE_ERROR=${request?.url} - ${error?.description}")
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        Log.e(TAG, "HTTP_ERROR=${errorResponse?.statusCode} - ${request?.url}")
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        Log.e(TAG, "SSL_ERROR=${error?.url} - $error")
                        super.onReceivedSslError(view, handler, error)
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        Log.e(TAG, "RENDER_PROCESS_GONE=didCrash:${detail?.didCrash()}")
                        view?.let {
                            val parent = it.parent as? ViewGroup
                            parent?.removeView(it)
                            it.destroy()
                        }
                        return true
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: ""
                        if (url.startsWith("intent://") ||
                            url.startsWith("youtube://") ||
                            url.startsWith("vnd.youtube://") ||
                            url.startsWith("market://")
                        ) {
                            return true
                        }
                        return false
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    allowContentAccess = true
                    allowFileAccess = true
                }

                addJavascriptInterface(AndroidJsBridge(), "AndroidBridge")

                tag = videoId
                Log.d(TAG, "VIDEO_ID=$videoId")
                Log.d(TAG, "EMBED_URL=$embedUrl")
                Log.d(TAG, "REFERER=$REFERER_HEADER")
                Log.d(TAG, "WEBVIEW_UA=${settings.userAgentString}")

                loadUrl(embedUrl, mapOf("Referer" to REFERER_HEADER))
            }
        },
        update = { webView ->
            if (webView.tag != videoId) {
                webView.tag = videoId
                webView.evaluateJavascript("if (window.__chordflySyncTimer) clearInterval(window.__chordflySyncTimer); window.__chordflyTimeSync = false;", null)
                webView.setBackgroundColor(Color.BLACK)
                Log.d(TAG, "VIDEO_ID=$videoId")
                Log.d(TAG, "EMBED_URL=$embedUrl")
                Log.d(TAG, "REFERER=$REFERER_HEADER")
                Log.d(TAG, "WEBVIEW_UA=${webView.settings.userAgentString}")

                webView.loadUrl(embedUrl, mapOf("Referer" to REFERER_HEADER))
            }
        },
        onRelease = { webView ->
            webView.evaluateJavascript("if (window.__chordflySyncTimer) clearInterval(window.__chordflySyncTimer);", null)
            webView.stopLoading()
            webView.destroy()
        },
        modifier = modifier
    )
}

fun extractYouTubeId(input: String): String {
    val clean = input.trim()
    return when {
        clean.contains("v=") -> clean.substringAfter("v=").substringBefore("&")
        clean.contains("youtu.be/") -> clean.substringAfter("youtu.be/").substringBefore("?")
        clean.length == 11 && !clean.contains(" ") -> clean
        else -> clean
    }
}
