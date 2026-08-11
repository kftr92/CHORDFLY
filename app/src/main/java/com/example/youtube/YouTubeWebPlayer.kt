package com.example.youtube

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "CHORDFLY_YOUTUBE"
private const val REFERER_BASE_URL = "https://com.aistudio.chordify.app"
private const val PLAYER_ORIGIN = "https://com.aistudio.chordify.app"

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

    val chromeClient = remember(activity) {
        FullscreenWebChromeClient(activity) { inFullscreen ->
            isFullscreen = inFullscreen
        }
    }

    BackHandler(enabled = isFullscreen) {
        chromeClient.onHideCustomView()
    }

    val videoId = remember(targetUrl) { extractYouTubeId(targetUrl) }

    val htmlContent = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { background: #000; width: 100vw; height: 100vh; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                #player { width: 100vw; height: 100vh; border: none; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                var timeInterval = null;

                function onYouTubeIframeAPIReady() {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.logMessage("PLAYER_API_READY");
                    }
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'controls': 1,
                            'playsinline': 1,
                            'rel': 0,
                            'enablejsapi': 1,
                            'origin': '$PLAYER_ORIGIN'
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError,
                            'onAutoplayBlocked': onAutoplayBlocked
                        }
                    });
                }

                function onPlayerReady(event) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.logMessage("PLAYER_READY");
                    }
                    try {
                        event.target.playVideo();
                    } catch(e) {}
                }

                function startTimeTracker() {
                    stopTimeTracker();
                    timeInterval = setInterval(function() {
                        if (player && typeof player.getCurrentTime === 'function' && window.AndroidBridge) {
                            var curTime = player.getCurrentTime();
                            if (curTime !== undefined) {
                                window.AndroidBridge.onTimeUpdate(curTime);
                            }
                        }
                    }, 200);
                }

                function stopTimeTracker() {
                    if (timeInterval) {
                        clearInterval(timeInterval);
                        timeInterval = null;
                    }
                }

                function sendCurrentTime() {
                    if (player && typeof player.getCurrentTime === 'function' && window.AndroidBridge) {
                        var curTime = player.getCurrentTime();
                        if (curTime !== undefined) {
                            window.AndroidBridge.onTimeUpdate(curTime);
                        }
                    }
                }

                function onPlayerStateChange(event) {
                    var state = event.data;
                    if (window.AndroidBridge) {
                        if (state === YT.PlayerState.PLAYING) {
                            window.AndroidBridge.logMessage("PLAYER_PLAYING");
                            window.AndroidBridge.onStateChange(true);
                            startTimeTracker();
                        } else if (state === YT.PlayerState.PAUSED) {
                            window.AndroidBridge.logMessage("PLAYER_PAUSED");
                            window.AndroidBridge.onStateChange(false);
                            stopTimeTracker();
                            sendCurrentTime();
                        } else if (state === YT.PlayerState.BUFFERING) {
                            window.AndroidBridge.logMessage("PLAYER_BUFFERING");
                        } else if (state === YT.PlayerState.ENDED) {
                            window.AndroidBridge.logMessage("PLAYER_ENDED");
                            window.AndroidBridge.onStateChange(false);
                            stopTimeTracker();
                            sendCurrentTime();
                        }
                    }
                }

                function onPlayerError(event) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onPlayerError(event.data);
                    }
                }

                function onAutoplayBlocked() {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.logMessage("PLAYER_AUTOPLAY_BLOCKED");
                        window.AndroidBridge.onAutoplayBlocked();
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webChromeClient = chromeClient
                webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
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
                    allowFileAccess = false
                    allowContentAccess = false
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onTimeUpdate(sec: Float) {
                        onTimeUpdate(sec)
                    }

                    @JavascriptInterface
                    fun onStateChange(playing: Boolean) {
                        onStateChange(playing)
                    }

                    @JavascriptInterface
                    fun onPlayerError(errorCode: Int) {
                        Log.e(TAG, "PLAYER_ERROR=$errorCode")
                    }

                    @JavascriptInterface
                    fun onAutoplayBlocked() {
                        Log.w(TAG, "PLAYER_AUTOPLAY_BLOCKED")
                    }

                    @JavascriptInterface
                    fun logMessage(msg: String) {
                        Log.d(TAG, msg)
                    }
                }, "AndroidBridge")

                tag = videoId
                Log.d(TAG, "VIDEO_ID=$videoId")
                Log.d(TAG, "REFERER_BASE_URL=$REFERER_BASE_URL")
                Log.d(TAG, "PLAYER_ORIGIN=$PLAYER_ORIGIN")
                loadDataWithBaseURL(REFERER_BASE_URL, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (webView.tag != videoId) {
                webView.tag = videoId
                Log.d(TAG, "VIDEO_ID=$videoId")
                Log.d(TAG, "REFERER_BASE_URL=$REFERER_BASE_URL")
                Log.d(TAG, "PLAYER_ORIGIN=$PLAYER_ORIGIN")
                webView.loadDataWithBaseURL(REFERER_BASE_URL, htmlContent, "text/html", "UTF-8", null)
            }
        },
        onRelease = { webView ->
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
        clean.contains("embed/") -> clean.substringAfter("embed/").substringBefore("?")
        clean.length == 11 && !clean.contains(" ") -> clean
        else -> clean
    }
}
