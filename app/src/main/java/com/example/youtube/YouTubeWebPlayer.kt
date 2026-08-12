package com.example.youtube

import android.annotation.SuppressLint
import android.view.View
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
    modifier: Modifier = Modifier
) {
    val rawInput = if (videoId.isNotBlank()) videoId else targetUrl
    val extractedId = extractYouTubeId(rawInput)
    val targetVideoId = extractedId.ifBlank { "jfKfPfyJRdk" }

    val htmlContent = remember(targetVideoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background: #000000; overflow: hidden; }
                #player { width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
                var player;
                var timer;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$targetVideoId',
                        playerVars: {
                            'playsinline': 1,
                            'controls': 1,
                            'rel': 0,
                            'enablejsapi': 1,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': function(event) {
                                if (player && player.getDuration) {
                                    AndroidBridge.onDurationReady(player.getDuration());
                                }
                            },
                            'onStateChange': function(event) {
                                if (event.data == YT.PlayerState.PLAYING) {
                                    if (player && player.getDuration) {
                                        AndroidBridge.onDurationReady(player.getDuration());
                                    }
                                    if (timer) clearInterval(timer);
                                    timer = setInterval(function() {
                                        if (player && player.getCurrentTime) {
                                            AndroidBridge.onTimeUpdate(player.getCurrentTime());
                                        }
                                    }, 200);
                                } else {
                                    if (timer) clearInterval(timer);
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
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onTimeUpdate(sec: Float) {
                        onTimeUpdate(sec)
                    }

                    @JavascriptInterface
                    fun onDurationReady(duration: Float) {
                        onDurationReady(duration)
                    }
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
