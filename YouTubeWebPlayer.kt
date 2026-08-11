package com.example.chordfly.youtube

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class YouTubeWebPlayer(
    private val onTime: (Float) -> Unit
) {
    @SuppressLint("SetJavaScriptEnabled")
    fun attach(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onTimeUpdate(seconds: Float) {
                onTime(seconds)
            }
        }, "ChordFlyBridge")
    }

    fun load(webView: WebView, videoId: String) {
        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
            html(videoId),
            "text/html",
            "UTF-8",
            null
        )
    }

    fun play(webView: WebView) {
        webView.evaluateJavascript("if(window.player){window.player.playVideo();}", null)
    }

    fun pause(webView: WebView) {
        webView.evaluateJavascript("if(window.player){window.player.pauseVideo();}", null)
    }

    private fun html(videoId: String): String = """
        <!doctype html>
        <html>
        <body style="margin:0;background:#000;overflow:hidden">
        <div id="player"></div>
        <script>
          var player;
          function onYouTubeIframeAPIReady() {
            player = new YT.Player('player', {
              width:'100%', height:'100%',
              videoId:'$videoId',
              playerVars:{playsinline:1,controls:1,rel:0},
              events:{onReady:function(){setInterval(function(){
                try {
                  if(player && player.getCurrentTime) {
                    ChordFlyBridge.onTimeUpdate(player.getCurrentTime());
                  }
                } catch(e){}
              }, 150);}}
            });
          }
        </script>
        <script src="https://www.youtube.com/iframe_api"></script>
        </body>
        </html>
    """.trimIndent()
}
