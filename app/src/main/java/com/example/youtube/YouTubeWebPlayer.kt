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
    targetUrl: String,
    onTimeUpdate: (Float) -> Unit,
    onStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(targetUrl) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { background: #000; width: 100vw; height: 100vh; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <iframe 
                id="ytplayer"
                src="$targetUrl" 
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                allowfullscreen>
            </iframe>
            <script>
                window.addEventListener('message', function(event) {
                    try {
                        var data = JSON.parse(event.data);
                        if (data.event === 'infoDelivery' && data.info) {
                            if (data.info.currentTime !== undefined && window.AndroidBridge) {
                                AndroidBridge.onTimeUpdate(data.info.currentTime);
                            }
                            if (data.info.playerState !== undefined && window.AndroidBridge) {
                                AndroidBridge.onStateChange(data.info.playerState === 1);
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
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: ""
                        if (url.contains("watch?v=")) {
                            val videoId = extractYouTubeId(url)
                            val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&rel=0&enablejsapi=1"
                            view?.loadUrl(embedUrl)
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
                }, "AndroidBridge")

                loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
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
