package com.example.chordfly

import android.Manifest
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chordfly.audio.AudioChordEngine
import com.example.chordfly.music.ChordTransposer
import com.example.chordfly.youtube.YouTubeWebPlayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B0D10)
                ) {
                    ChordFlyScreen()
                }
            }
        }
    }
}

@Composable
private fun ChordFlyScreen(vm: MainViewModel = viewModel()) {
    val state by vm.ui.collectAsState()
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) recording = true
    }

    val audioEngine = remember {
        AudioChordEngine(context) { chord -> vm.addDspChord(chord) }
    }

    DisposableEffect(Unit) {
        onDispose { audioEngine.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D10))
            .padding(12.dp)
    ) {
        Text(
            "CHORDFLY V2",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("YouTube URL / Video ID") }
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    extractYouTubeId(url)?.let(vm::setVideoId)
                }
            ) {
                Icon(Icons.Default.Search, contentDescription = "Load")
            }
        }

        Spacer(Modifier.height(8.dp))

        Row {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::setTitle,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Judul lagu") }
            )
            Spacer(Modifier.width(6.dp))
            OutlinedTextField(
                value = state.artist,
                onValueChange = vm::setArtist,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Artis") }
            )
        }

        Spacer(Modifier.height(10.dp))

        if (state.videoId.isNotBlank()) {
            val player = remember {
                YouTubeWebPlayer { vm.setTime(it) }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                factory = { viewContext ->
                    WebView(viewContext).also {
                        player.attach(it)
                        player.load(it, state.videoId)
                    }
                }
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(Color.Black, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Masukkan URL YouTube", color = Color.Gray)
            }
        }

        Spacer(Modifier.height(10.dp))

        val activeChord = state.chords.getOrNull(state.activeIndex)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171B21))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    activeChord?.let {
                        ChordTransposer.transpose(it.chord, state.transpose)
                    } ?: "—",
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "NOW  •  ${"%.2f".format(state.currentTime)} s",
                    color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.transpose(-1) }) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text("${state.transpose} semitone", color = Color.White)
                    TextButton(onClick = { vm.transpose(1) }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.chords.forEachIndexed { index, chord ->
                val selected = index == state.activeIndex
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFF304B6E)
                        else Color(0xFF171B21)
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            ChordTransposer.transpose(chord.chord, state.transpose),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${"%.1f".format(chord.timeSec)}s",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (recording) {
                        audioEngine.stop()
                        recording = false
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        audioEngine.start()
                    }
                }
            ) {
                Icon(
                    if (recording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(if (recording) "Stop DSP" else "Deteksi Mic")
            }

            Button(
                enabled = !state.isAnalyzing,
                onClick = vm::analyzeWithGemini
            ) {
                Text(if (state.isAnalyzing) "Menganalisis..." else "Koreksi Gemini")
            }
        }

        if (state.key != null || state.bpm != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Key: ${state.key ?: "?"}   BPM: ${state.bpm ?: "?"}",
                color = Color.Gray
            )
        }

        state.error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Color(0xFFFF8A80))
        }
    }
}

private fun extractYouTubeId(input: String): String? {
    val value = input.trim()
    if (value.matches(Regex("[A-Za-z0-9_-]{11}"))) return value

    val patterns = listOf(
        Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
        Regex("""youtube\.com/watch\?v=([A-Za-z0-9_-]{11})"""),
        Regex("""youtube\.com/embed/([A-Za-z0-9_-]{11})"""),
        Regex("""youtube\.com/shorts/([A-Za-z0-9_-]{11})""")
    )

    return patterns.firstNotNullOfOrNull { it.find(value)?.groupValues?.getOrNull(1) }
}
