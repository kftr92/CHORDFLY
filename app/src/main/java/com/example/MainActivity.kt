package com.example

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ChordTimelineGrid
import com.example.ui.CurrentChordCard
import com.example.ui.HeaderBar
import com.example.ui.NextChordsRow
import com.example.ui.TransportControls
import com.example.youtube.YouTubeSearchWebView
import com.example.youtube.YouTubeWebPlayer

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B0D10)
                ) {
                    ChordFlyScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ChordFlyScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val youtubeState by viewModel.youtubeState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleMicListening()
        }
    }

    // Collect UI events (Toasts)
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is YouTubeUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (youtubeState.showYouTubeSearch) {
        YouTubeSearchWebView(
            query = youtubeState.searchQuery,
            onVideoSelected = { videoId ->
                viewModel.selectYouTubeVideo(videoId)
            },
            onClose = {
                viewModel.closeYouTubeSearch()
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
        // Top Header
        HeaderBar(
            title = uiState.title,
            artist = uiState.artist,
            key = uiState.key,
            bpm = uiState.bpm,
            isAiAnalyzing = uiState.isAiAnalyzing,
            onAiAnalyzeClick = { viewModel.runGeminiChordAnalysis() }
        )

        // CHORDFLY V2 SEARCH & PLAYBACK CONTROL PANEL
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141A24)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Section 1: Search Song Name
                Text(
                    text = "Cari lagu",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = youtubeState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("misal: Peterpan Bintang di Surga", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.openYouTubeSearch() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FF88),
                        unfocusedBorderColor = Color(0xFF232832),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { viewModel.openYouTubeSearch() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari di YouTube",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔎 CARI DI YOUTUBE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Color(0xFF222B38)
                )

                // Section 2: Paste YouTube URL
                Text(
                    text = "Atau tempel URL video:",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = youtubeState.youtubeUrl,
                    onValueChange = { viewModel.onYoutubeUrlChange(it) },
                    placeholder = { Text("https://youtube.com/watch?v=...", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FF88),
                        unfocusedBorderColor = Color(0xFF232832),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Paste Button
                    OutlinedButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboardManager.primaryClip
                            val clipText = if (clipData != null && clipData.itemCount > 0) {
                                clipData.getItemAt(0).text?.toString()
                            } else {
                                null
                            }
                            viewModel.pasteFromClipboard(clipText)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF88)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Tempel Clipboard",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📋 TEMPEL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Open Video Button
                    Button(
                        onClick = { viewModel.openVideoFromUrl() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Buka Video",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("▶ BUKA VIDEO", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status Bar
                Surface(
                    color = Color(0xFF0D121A),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Status: ${youtubeState.status}",
                        color = if (youtubeState.status.contains("salah", true) || youtubeState.status.contains("tidak", true) || youtubeState.status.contains("Gagal", true)) Color(0xFFFF6B6B) else Color(0xFF00FF88),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // YouTube Player Container
        YouTubeWebPlayer(
            targetUrl = uiState.activeTargetUrl,
            onTimeUpdate = { sec -> viewModel.updatePlaybackTime(sec) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        // Current Active Chord Hero Card
        CurrentChordCard(
            currentChord = uiState.currentChord,
            transposeOffset = uiState.transposeOffset,
            currentTimeSec = uiState.currentTimeSec,
            notes = uiState.currentChordNotes,
            livePitch = uiState.livePitch,
            isMicListening = uiState.isMicListening,
            onMicToggleClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    viewModel.toggleMicListening()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )

        // Upcoming Chords Preview Row
        NextChordsRow(
            nextChords = uiState.nextChords,
            transposeOffset = uiState.transposeOffset
        )

        // Key Transpose & Status Control Bar
        TransportControls(
            transposeOffset = uiState.transposeOffset,
            onIncrement = { viewModel.incrementTranspose() },
            onDecrement = { viewModel.decrementTranspose() },
            onReset = { viewModel.resetTranspose() },
            aiStatusMessage = uiState.aiStatusMessage
        )

        // Full Sync Chord Timeline Grid
        ChordTimelineGrid(
            chords = uiState.chords,
            activeChord = uiState.currentChord,
            transposeOffset = uiState.transposeOffset,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }
}
}
