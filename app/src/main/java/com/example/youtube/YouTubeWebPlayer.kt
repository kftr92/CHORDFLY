package com.example.youtube

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebPlayer(
    videoId: String = "",
    targetUrl: String = "",
    onTimeUpdate: (Float) -> Unit,
    onDurationReady: (Float) -> Unit = {},
    onStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effectiveVideoId = remember(videoId, targetUrl) {
        if (videoId.isNotBlank()) videoId else extractYouTubeId(targetUrl)
    }

    val htmlContent = remember(effectiveVideoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { background: #000; height: 100vh; overflow: hidden; }
                #player { width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$effectiveVideoId',
                        playerVars: { 'playsinline': 1, 'controls': 1, 'enablejsapi': 1, 'rel': 0 },
                        events: {
                            'onReady': function(e) {
                                if (player && player.getDuration) {
                                    AndroidBridge.onDurationReady(player.getDuration());
                                }
                            },
                            'onStateChange': function(e) {
                                if (e.data == YT.PlayerState.PLAYING) {
                                    if (player && player.getDuration) {
                                        AndroidBridge.onDurationReady(player.getDuration());
                                    }
                                    setInterval(function() {
                                        if (player && player.getCurrentTime) {
                                            AndroidBridge.onTimeUpdate(player.getCurrentTime());
                                        }
                                    }, 200);
                                    AndroidBridge.onStateChange(true);
                                } else if (e.data == YT.PlayerState.PAUSED) {
                                    AndroidBridge.onStateChange(false);
                                }
                            }
                        }
                    });
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onTimeUpdate(sec: Float) { onTimeUpdate(sec) }

                    @JavascriptInterface
                    fun onDurationReady(duration: Float) { onDurationReady(duration) }

                    @JavascriptInterface
                    fun onStateChange(playing: Boolean) { onStateChange(playing) }
                }, "AndroidBridge")
                loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
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
