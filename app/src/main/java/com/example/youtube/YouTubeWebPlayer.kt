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
    modifier: Modifier = Modifier
) {
    val cleanVideoId = remember(videoId, targetUrl) {
        val raw = if (videoId.isNotBlank()) videoId else targetUrl
        extractYouTubeId(raw)
    }

    val embedUrl = "https://www.youtube-nocookie.com/embed/$cleanVideoId?enablejsapi=1&autoplay=1&controls=1&playsinline=1&rel=0&modestbranding=1"

    val htmlContent = remember(cleanVideoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { background: #000; width: 100vw; height: 100vh; overflow: hidden; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <iframe id="player" src="$embedUrl" allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe>
            <script>
                // Bridge Listener untuk menangkap event dari YouTube IFrame Embed
                window.addEventListener('message', function(event) {
                    try {
                        var data = JSON.parse(event.data);
                        if (data.event === 'infoDelivery' && data.info) {
                            if (data.info.currentTime !== undefined) {
                                AndroidBridge.onTimeUpdate(data.info.currentTime);
                            }
                            if (data.info.duration !== undefined) {
                                AndroidBridge.onDurationReady(data.info.duration);
                            }
                        }
                    } catch(e) {}
                });
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
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        // Kunci navigasi internal agar tidak pernah membuka aplikasi YouTube luar
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
                    fun onTimeUpdate(sec: Float) { onTimeUpdate(sec) }

                    @JavascriptInterface
                    fun onDurationReady(duration: Float) { onDurationReady(duration) }
                }, "AndroidBridge")

                loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
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
