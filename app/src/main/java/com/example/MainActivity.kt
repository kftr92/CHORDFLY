package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class ChordTimestamp(val id: Int, val chord: String, val timeSec: Float)

object ChordTransposer {
    private val NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun transpose(chord: String, semitones: Int): String {
        if (semitones == 0) return chord
        val baseNote = when {
            chord.length >= 2 && (chord[1] == '#' || chord[1] == 'b') -> chord.substring(0, 2)
            else -> chord.substring(0, 1)
        }
        val normalizedNote = when (baseNote) {
            "Db" -> "C#"; "Eb" -> "D#"; "Gb" -> "F#"; "Ab" -> "G#"; "Bb" -> "A#"; else -> baseNote
        }
        val index = NOTES.indexOf(normalizedNote)
        if (index == -1) return chord
        var newIndex = (index + semitones) % NOTES.size
        if (newIndex < 0) newIndex += NOTES.size
        val suffix = chord.removePrefix(baseNote)
        return NOTES[newIndex] + suffix
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    ChordifyAppScreen()
                }
            }
        }
    }
}

@Composable
fun ChordifyAppScreen() {
    // State query bisa berupa Video ID atau Kata Kunci Pencarian
    var searchQueryOrId by remember { mutableStateOf("jfKfPfyJRdk") }
    var isSearchQuery by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var currentTimeSec by remember { mutableFloatStateOf(0f) }
    var transposeOffset by remember { mutableIntStateOf(0) }

    val chordsList = remember {
        listOf(
            ChordTimestamp(0, "C", 0.0f), ChordTimestamp(1, "G", 3.0f),
            ChordTimestamp(2, "Am", 6.0f), ChordTimestamp(3, "F", 9.0f),
            ChordTimestamp(4, "C", 12.0f), ChordTimestamp(5, "G", 15.0f),
            ChordTimestamp(6, "Am", 18.0f), ChordTimestamp(7, "F", 21.0f),
            ChordTimestamp(8, "C", 24.0f), ChordTimestamp(9, "Em", 27.0f),
            ChordTimestamp(10, "F", 30.0f), ChordTimestamp(11, "G", 33.0f)
        )
    }

    val activeIndex = remember(currentTimeSec) {
        var idx = 0
        for (i in chordsList.indices.reversed()) {
            if (currentTimeSec >= chordsList[i].timeSec) {
                idx = i
                break
            }
        }
        idx
    }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeIndex) {
        coroutineScope.launch {
            if (activeIndex in chordsList.indices) {
                gridState.animateScrollToItem(activeIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Bar Pencarian Tunggal
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Ketik Judul Lagu / Paste Link YouTube...", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF88),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (searchInput.isNotBlank()) {
                        val extractedId = extractYouTubeId(searchInput)
                        if (extractedId != searchInput.trim()) {
                            // Jika input berupa Link/ID YouTube
                            searchQueryOrId = extractedId
                            isSearchQuery = false
                        } else {
                            // Jika input berupa Judul Lagu / Kata Kunci
                            searchQueryOrId = searchInput.trim()
                            isSearchQuery = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88))
            ) {
                Icon(Icons.Default.Search, contentDescription = "Cari", tint = Color.Black)
            }
        }

        // YouTube Embed Player Bersih (Tanpa Search Bar Internal YouTube)
        CleanYouTubePlayer(
            queryOrId = searchQueryOrId,
            isSearch = isSearchQuery,
            onTimeUpdate = { time -> currentTimeSec = time },
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )

        // Control Bar (Transpose & Time Display)
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%.1f s", currentTimeSec),
                color = Color(0xFF00FF88),
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Key: ${if (transposeOffset > 0) "+$transposeOffset" else transposeOffset}", color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { if (transposeOffset > -12) transposeOffset-- }) {
                    Icon(Icons.Default.Remove, contentDescription = "Down", tint = Color.White)
                }
                IconButton(onClick = { transposeOffset = 0 }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.Gray)
                }
                IconButton(onClick = { if (transposeOffset < 12) transposeOffset++ }) {
                    Icon(Icons.Default.Add, contentDescription = "Up", tint = Color.White)
                }
            }
        }

        // Chord Grid Sync
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp)
        ) {
            itemsIndexed(chordsList) { index, item ->
                val isActive = index == activeIndex
                val transposed = ChordTransposer.transpose(item.chord, transposeOffset)
                val bgColor by animateColorAsState(if (isActive) Color(0xFF00FF88) else Color(0xFF252525), label = "bg")
                val textColor by animateColorAsState(if (isActive) Color.Black else Color.White, label = "text")

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(1.dp, if (isActive) Color.White else Color(0xFF333333), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = transposed, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text(text = String.format("%.1fs", item.timeSec), fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CleanYouTubePlayer(
    queryOrId: String,
    isSearch: Boolean,
    onTimeUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val embedSrc = if (isSearch) {
        val encodedQuery = URLEncoder.encode(queryOrId, "UTF-8")
        "https://www.youtube.com/embed?listType=search&list=$encodedQuery&autoplay=1&controls=1&modestbranding=1&rel=0"
    } else {
        "https://www.youtube.com/embed/$queryOrId?autoplay=1&controls=1&modestbranding=1&rel=0&enablejsapi=1"
    }

    val htmlContent = remember(embedSrc) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { background: #000; height: 100vh; overflow: hidden; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <iframe 
                id="ytplayer"
                src="$embedSrc" 
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                allowfullscreen>
            </iframe>
            <script>
                // Basic time listener bridge
                window.addEventListener('message', function(event) {
                    try {
                        var data = JSON.parse(event.data);
                        if (data.event === 'infoDelivery' && data.info && data.info.currentTime) {
                            AndroidBridge.onTimeUpdate(data.info.currentTime);
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
