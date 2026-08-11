package com.example.youtube

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "CHORDFLY_YOUTUBE"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeSearchWebView(
    query: String,
    onVideoSelected: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf(query) }

    val searchUrl = remember(query) {
        val encodedQuery = Uri.encode(query.trim())
        val url = "https://www.youtube.com/results?search_query=$encodedQuery"
        Log.d(TAG, "SEARCH_QUERY=$query")
        Log.d(TAG, "WEBVIEW_URL=$url")
        url
    }

    // Intercept physical Back button
    BackHandler {
        val webView = webViewInstance
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            onClose()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D10))
    ) {
        // Top Navigation Bar
        Surface(
            color = Color(0xFF141A24),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Go Previous Button
                IconButton(
                    onClick = {
                        val webView = webViewInstance
                        if (webView != null && webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            onClose()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                // Title / Query
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pageTitle.ifBlank { query.ifBlank { "YouTube Search" } },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Refresh Button
                IconButton(
                    onClick = { webViewInstance?.reload() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Muat Ulang",
                        tint = Color.LightGray
                    )
                }

                // Close Button (X)
                IconButton(
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }
            }
        }

        // Loading Progress Indicator
        AnimatedVisibility(visible = isLoading) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF00FF88),
                    trackColor = Color(0xFF1C2533)
                )
                Surface(
                    color = Color(0xFF141A24),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Loading YouTube...",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Error State UI
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2636)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to load YouTube.",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Periksa koneksi internet Anda dan coba lagi.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                hasError = false
                                isLoading = true
                                webViewInstance?.loadUrl(searchUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RETRY", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Main Android WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewInstance = this
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (newProgress >= 90) {
                                    isLoading = false
                                }
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            private fun checkForVideoSelection(url: String?): Boolean {
                                if (url.isNullOrBlank()) return false

                                val cleanUrl = url
                                Log.d(TAG, "WEBVIEW_URL=$cleanUrl")
                                currentUrl = cleanUrl

                                // Intercept deep-link app intents (youtube://, intent://, etc.) so app is never launched
                                if (cleanUrl.startsWith("intent://") ||
                                    cleanUrl.startsWith("youtube://") ||
                                    cleanUrl.startsWith("vnd.youtube://") ||
                                    cleanUrl.startsWith("market://")
                                ) {
                                    Log.d(TAG, "Intercepted external app scheme: $cleanUrl")
                                    return true
                                }

                                // Check if user navigated to a watch/shorts/embed URL
                                val videoId = YouTubeUrlParser.extractVideoId(cleanUrl)
                                if (videoId != null && (cleanUrl.contains("watch?") || cleanUrl.contains("shorts/") || cleanUrl.contains("youtu.be/"))) {
                                    Log.d(TAG, "VIDEO_ID_DETECTED=$videoId")
                                    onVideoSelected(videoId)
                                    return true
                                }

                                return false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString()
                                return checkForVideoSelection(url)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                hasError = false
                                checkForVideoSelection(url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                checkForVideoSelection(url)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    isLoading = false
                                }
                            }
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            allowFileAccess = false
                            allowContentAccess = false
                            mediaPlaybackRequiresUserGesture = false
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        loadUrl(searchUrl)
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.destroy()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
