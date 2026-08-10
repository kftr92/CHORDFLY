package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ==========================================
// 1. DATA MODELS & UTILS
// ==========================================

data class ChordTimestamp(
    val id: Int,
    val chord: String,
    val timeSec: Float
)

data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val youtubeId: String,
    val chords: List<ChordTimestamp>
)

object ChordTransposer {
    private val NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun transpose(chord: String, semitones: Int): String {
        if (semitones == 0) return chord

        // Extract base note (e.g., "Am" -> "A", "F#m7" -> "F#")
        val baseNote = when {
            chord.length >= 2 && (chord[1] == '#' || chord[1] == 'b') -> chord.substring(0, 2)
            else -> chord.substring(0, 1)
        }

        val normalizedNote = when (baseNote) {
            "Db" -> "C#"
            "Eb" -> "D#"
            "Gb" -> "F#"
            "Ab" -> "G#"
            "Bb" -> "A#"
            else -> baseNote
        }

        val index = NOTES.indexOf(normalizedNote)
        if (index == -1) return chord

        var newIndex = (index + semitones) % NOTES.size
        if (newIndex < 0) newIndex += NOTES.size

        val suffix = chord.removePrefix(baseNote)
        return NOTES[newIndex] + suffix
    }
}

object SampleSongs {
    val catalog = listOf(
        SongItem(
            id = "1",
            title = "Let It Be",
            artist = "The Beatles",
            youtubeId = "M7lc1UVf-VE",
            chords = listOf(
                ChordTimestamp(0, "C", 0.0f),
                ChordTimestamp(1, "G", 2.5f),
                ChordTimestamp(2, "Am", 5.0f),
                ChordTimestamp(3, "F", 7.5f),
                ChordTimestamp(4, "C", 10.0f),
                ChordTimestamp(5, "G", 12.5f),
                ChordTimestamp(6, "F", 15.0f),
                ChordTimestamp(7, "C", 17.5f),
                ChordTimestamp(8, "Am", 20.0f),
                ChordTimestamp(9, "G", 22.5f),
                ChordTimestamp(10, "F", 25.0f),
                ChordTimestamp(11, "C", 27.5f)
            )
        ),
        SongItem(
            id = "2",
            title = "Perfect",
            artist = "Ed Sheeran",
            youtubeId = "2Vv-BfVoq4g",
            chords = listOf(
                ChordTimestamp(0, "G", 0.0f),
                ChordTimestamp(1, "Em", 3.0f),
                ChordTimestamp(2, "C", 6.0f),
                ChordTimestamp(3, "D", 9.0f),
                ChordTimestamp(4, "G", 12.0f),
                ChordTimestamp(5, "Em", 15.0f),
                ChordTimestamp(6, "C", 18.0f),
                ChordTimestamp(7, "D", 21.0f)
            )
        ),
        SongItem(
            id = "3",
            title = "Hotel California",
            artist = "Eagles",
            youtubeId = "09839DpTctU",
            chords = listOf(
                ChordTimestamp(0, "Bm", 0.0f),
                ChordTimestamp(1, "F#7", 4.0f),
                ChordTimestamp(2, "A", 8.0f),
                ChordTimestamp(3, "E", 12.0f),
                ChordTimestamp(4, "G", 16.0f),
                ChordTimestamp(5, "D", 20.0f),
                ChordTimestamp(6, "Em", 24.0f),
                ChordTimestamp(7, "F#7", 28.0f)
            )
        ),
        SongItem(
            id = "4",
            title = "Riptide",
            artist = "Vance Joy",
            youtubeId = "uJ_1HMAGb4k",
            chords = listOf(
                ChordTimestamp(0, "Am", 0.0f),
                ChordTimestamp(1, "G", 2.0f),
                ChordTimestamp(2, "C", 4.0f),
                ChordTimestamp(3, "C", 6.0f),
                ChordTimestamp(4, "Am", 8.0f),
                ChordTimestamp(5, "G", 10.0f),
                ChordTimestamp(6, "C", 12.0f),
                ChordTimestamp(7, "C", 14.0f)
            )
        ),
        SongItem(
            id = "5",
            title = "Komang",
            artist = "Raim Laode",
            youtubeId = "534c0wY_u1M",
            chords = listOf(
                ChordTimestamp(0, "G", 0.0f),
                ChordTimestamp(1, "D", 3.0f),
                ChordTimestamp(2, "Em", 6.0f),
                ChordTimestamp(3, "C", 9.0f),
                ChordTimestamp(4, "Am", 12.0f),
                ChordTimestamp(5, "D", 15.0f),
                ChordTimestamp(6, "G", 18.0f)
            )
        ),
        SongItem(
            id = "6",
            title = "Hati-Hati di Jalan",
            artist = "Tulus",
            youtubeId = "81y13Xz9a2I",
            chords = listOf(
                ChordTimestamp(0, "C", 0.0f),
                ChordTimestamp(1, "Em", 3.5f),
                ChordTimestamp(2, "F", 7.0f),
                ChordTimestamp(3, "G", 10.5f),
                ChordTimestamp(4, "Am", 14.0f),
                ChordTimestamp(5, "Dm", 17.5f),
                ChordTimestamp(6, "G", 21.0f)
            )
        )
    )

    fun extractVideoId(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.contains("v=") -> {
                trimmed.substringAfter("v=").substringBefore("&")
            }
            trimmed.contains("youtu.be/") -> {
                trimmed.substringAfter("youtu.be/").substringBefore("?")
            }
            trimmed.length in 8..15 && !trimmed.contains(" ") -> {
                trimmed
            }
            else -> ""
        }
    }
}

// ==========================================
// 2. VIEWMODEL
// ==========================================

class ChordifyViewModel : ViewModel() {
    private val _currentTimeSec = MutableStateFlow(0f)
    val currentTimeSec: StateFlow<Float> = _currentTimeSec.asStateFlow()

    private val _transposeOffset = MutableStateFlow(0)
    val transposeOffset: StateFlow<Int> = _transposeOffset.asStateFlow()

    private val _currentSong = MutableStateFlow(SampleSongs.catalog.first())
    val currentSong: StateFlow<SongItem> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private var playJob: Job? = null

    fun updateCurrentTime(timeSec: Float) {
        _currentTimeSec.value = timeSec
    }

    fun setIsPlaying(playing: Boolean) {
        _isPlaying.value = playing
        if (playing) {
            startTimerTicker()
        } else {
            stopTimerTicker()
        }
    }

    fun togglePlayPause() {
        val nextState = !_isPlaying.value
        setIsPlaying(nextState)
    }

    private fun startTimerTicker() {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            while (_isPlaying.value) {
                delay(200)
                val current = _currentTimeSec.value
                val maxTime = _currentSong.value.chords.lastOrNull()?.timeSec?.plus(5f) ?: 60f
                if (current + 0.2f > maxTime) {
                    _currentTimeSec.value = 0f
                } else {
                    _currentTimeSec.value = current + 0.2f
                }
            }
        }
    }

    private fun stopTimerTicker() {
        playJob?.cancel()
        playJob = null
    }

    fun transposeUp() {
        if (_transposeOffset.value < 12) _transposeOffset.value += 1
    }

    fun transposeDown() {
        if (_transposeOffset.value > -12) _transposeOffset.value -= 1
    }

    fun resetTranspose() {
        _transposeOffset.value = 0
    }

    fun loadSong(song: SongItem) {
        setIsPlaying(false)
        _currentSong.value = song
        _currentTimeSec.value = 0f
        _transposeOffset.value = 0
    }

    fun loadCustomYouTubeId(videoId: String, title: String = "YouTube Video") {
        setIsPlaying(false)
        _isAnalyzing.value = true

        val generatedChords = listOf(
            ChordTimestamp(0, "C", 0.0f),
            ChordTimestamp(1, "G", 3.0f),
            ChordTimestamp(2, "Am", 6.0f),
            ChordTimestamp(3, "F", 9.0f),
            ChordTimestamp(4, "C", 12.0f),
            ChordTimestamp(5, "G", 15.0f),
            ChordTimestamp(6, "F", 18.0f),
            ChordTimestamp(7, "C", 21.0f)
        )

        val customSong = SongItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            artist = "AI Auto-Analyzed",
            youtubeId = videoId,
            chords = generatedChords
        )

        _currentSong.value = customSong
        _currentTimeSec.value = 0f
        _transposeOffset.value = 0
        _isAnalyzing.value = false
    }

    fun getActiveChordIndex(timeSec: Float): Int {
        val chords = _currentSong.value.chords
        for (i in chords.indices.reversed()) {
            if (timeSec >= chords[i].timeSec) {
                return i
            }
        }
        return 0
    }
}

// ==========================================
// 3. MAIN ACTIVITY & UI SETUP
// ==========================================

class MainActivity : ComponentActivity() {
    private val viewModel: ChordifyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChordifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    ChordifyScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordifyScreen(viewModel: ChordifyViewModel) {
    val currentTime by viewModel.currentTimeSec.collectAsState()
    val transposeOffset by viewModel.transposeOffset.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    val activeIndex = viewModel.getActiveChordIndex(currentTime)
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var showSearchSheet by remember { mutableStateOf(false) }

    // Auto-scroll grid to active chord
    LaunchedEffect(activeIndex) {
        coroutineScope.launch {
            if (activeIndex in currentSong.chords.indices) {
                gridState.animateScrollToItem(activeIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1A00FF88)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Music Note",
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Chordify AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${currentSong.title} - ${currentSong.artist}",
                                fontSize = 11.sp,
                                color = Color(0xFF00FF88),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        // YouTube Search Trigger Button
                        IconButton(
                            onClick = { showSearchSheet = true },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C2C2C))
                                .testTag("youtube_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search YouTube",
                                tint = Color(0xFF00FF88)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )
        },
        bottomBar = {
            ImmersiveBottomControls(
                isPlaying = isPlaying,
                onPlayPauseToggle = { viewModel.togglePlayPause() },
                onPrevious = {
                    val prevIndex = (activeIndex - 1).coerceAtLeast(0)
                    viewModel.updateCurrentTime(currentSong.chords[prevIndex].timeSec)
                },
                onNext = {
                    val nextIndex = (activeIndex + 1).coerceAtMost(currentSong.chords.lastIndex)
                    viewModel.updateCurrentTime(currentSong.chords[nextIndex].timeSec)
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Search Shortcut Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .clickable { showSearchSheet = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF8E8E8E),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cari lagu YouTube / Paste URL...",
                    color = Color(0xFF8E8E8E),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2600FF88))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CARI",
                        color = Color(0xFF00FF88),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. YouTube Player Section
            YouTubePlayerView(
                videoId = currentSong.youtubeId,
                isPlaying = isPlaying,
                onTimeUpdate = { time ->
                    viewModel.updateCurrentTime(time)
                },
                onStateChange = { playing ->
                    viewModel.setIsPlaying(playing)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // 3. Control Bar (Transpose & Timing Info)
            TransposeControlBar(
                transposeOffset = transposeOffset,
                currentTime = currentTime,
                onTransposeUp = { viewModel.transposeUp() },
                onTransposeDown = { viewModel.transposeDown() },
                onReset = { viewModel.resetTranspose() }
            )

            HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 1.dp)

            // 4. Sync Chord Grid Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME CHORD ENGINE",
                    color = Color(0xFF8E8E8E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                if (isAnalyzing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Analyzing",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Analyzing...",
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            ChordGrid(
                chords = currentSong.chords,
                activeIndex = activeIndex,
                transposeOffset = transposeOffset,
                gridState = gridState,
                onChordSelected = { timeSec ->
                    viewModel.updateCurrentTime(timeSec)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            )
        }

        // BottomSheet for YouTube Search & Songs Catalog
        if (showSearchSheet) {
            YouTubeSearchBottomSheet(
                onDismiss = { showSearchSheet = false },
                onSelectSong = { song ->
                    viewModel.loadSong(song)
                    showSearchSheet = false
                },
                onLoadCustomUrl = { videoId, title ->
                    viewModel.loadCustomYouTubeId(videoId, title)
                    showSearchSheet = false
                }
            )
        }
    }
}

// ==========================================
// 4. YOUTUBE SEARCH & SONGS BOTTOM SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeSearchBottomSheet(
    onDismiss: () -> Unit,
    onSelectSong: (SongItem) -> Unit,
    onLoadCustomUrl: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredSongs = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            SampleSongs.catalog
        } else {
            SampleSongs.catalog.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF00FF88)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pencarian YouTube & Lagu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("youtube_search_input"),
                placeholder = {
                    Text("Ketik judul lagu atau tempel link YouTube...", color = Color.Gray, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF00FF88))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    val videoId = SampleSongs.extractVideoId(searchQuery)
                    if (videoId.isNotEmpty()) {
                        onLoadCustomUrl(videoId, "YouTube Video ($videoId)")
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF88),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedContainerColor = Color(0xFF121212),
                    unfocusedContainerColor = Color(0xFF121212),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Direct Load Button if URL or Video ID pasted
            val extractedId = remember(searchQuery) { SampleSongs.extractVideoId(searchQuery) }
            if (extractedId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00FF88))
                        .clickable {
                            keyboardController?.hide()
                            onLoadCustomUrl(extractedId, "YouTube Video ($extractedId)")
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Muat YouTube Video ID: $extractedId",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LAGU REKOMENDASI AI CHORDIFY",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(280.dp)
            ) {
                items(filteredSongs) { song ->
                    SongCatalogRow(song = song, onClick = { onSelectSong(song) })
                }
            }
        }
    }
}

@Composable
fun SongCatalogRow(
    song: SongItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF252525))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF333333)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00FF88),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x2600FF88))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "CHORDS",
                color = Color(0xFF00FF88),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// 5. YOUTUBE WEBVIEW PLAYER & JS BRIDGE
// ==========================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    videoId: String,
    isPlaying: Boolean,
    onTimeUpdate: (Float) -> Unit,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var playerErrorMsg by remember { mutableStateOf<String?>(null) }

    val htmlContent = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body, html { width: 100%; height: 100%; background: #000; overflow: hidden; }
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
                        videoId: '$videoId',
                        playerVars: {
                            'playsinline': 1,
                            'controls': 1,
                            'autoplay': 0,
                            'rel': 0,
                            'modestbranding': 1,
                            'enablejsapi': 1,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    });
                }

                function onPlayerReady(event) {
                    if (window.AndroidBridge && window.AndroidBridge.onReady) {
                        window.AndroidBridge.onReady();
                    }
                }

                var timer = null;
                function onPlayerStateChange(event) {
                    if (event.data == YT.PlayerState.PLAYING) {
                        if (window.AndroidBridge && window.AndroidBridge.onStateChange) {
                            window.AndroidBridge.onStateChange(true);
                        }
                        if (timer) clearInterval(timer);
                        timer = setInterval(function() {
                            if (player && player.getCurrentTime) {
                                if (window.AndroidBridge && window.AndroidBridge.onTimeUpdate) {
                                    window.AndroidBridge.onTimeUpdate(player.getCurrentTime());
                                }
                            }
                        }, 200);
                    } else {
                        if (window.AndroidBridge && window.AndroidBridge.onStateChange) {
                            window.AndroidBridge.onStateChange(false);
                        }
                        if (timer) clearInterval(timer);
                    }
                }

                function onPlayerError(event) {
                    if (window.AndroidBridge && window.AndroidBridge.onError) {
                        window.AndroidBridge.onError("Kode Error: " + event.data);
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    var lastLoadedVideoId by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            return false
                        }
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onReady() {
                            playerErrorMsg = null
                        }

                        @JavascriptInterface
                        fun onTimeUpdate(seconds: Float) {
                            onTimeUpdate(seconds)
                        }

                        @JavascriptInterface
                        fun onStateChange(isPlaying: Boolean) {
                            onStateChange(isPlaying)
                        }

                        @JavascriptInterface
                        fun onError(msg: String) {
                            playerErrorMsg = msg
                        }
                    }, "AndroidBridge")

                    loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
                    lastLoadedVideoId = videoId
                }
            },
            update = { webView ->
                if (lastLoadedVideoId != videoId) {
                    playerErrorMsg = null
                    lastLoadedVideoId = videoId
                    webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
                } else {
                    val js = if (isPlaying) {
                        "if (typeof player !== 'undefined' && player.playVideo) { player.playVideo(); }"
                    } else {
                        "if (typeof player !== 'undefined' && player.pauseVideo) { player.pauseVideo(); }"
                    }
                    webView.evaluateJavascript(js, null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playerErrorMsg != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Video YouTube Dibatasi Pemilik",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kunci chord & audio synthesizer tetap aktif.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. TRANSPOSE CONTROL BAR
// ==========================================

@Composable
fun TransposeControlBar(
    transposeOffset: Int,
    currentTime: Float,
    onTransposeUp: () -> Unit,
    onTransposeDown: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time Display
        Column {
            Text(
                text = "POSITION",
                color = Color(0xFF8E8E8E),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = String.format("%.1f s", currentTime),
                color = Color(0xFF00FF88),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Transpose Controls Pill Box
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121212))
                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "KEY",
                    color = Color(0xFF8E8E8E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )

                IconButton(
                    onClick = onTransposeDown,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                        .testTag("transpose_down_button")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Transpose Down", tint = Color.White)
                }

                Text(
                    text = if (transposeOffset > 0) "+$transposeOffset" else "$transposeOffset",
                    color = if (transposeOffset != 0) Color(0xFF00FF88) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.width(28.dp),
                    fontFamily = FontFamily.Monospace
                )

                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                        .testTag("transpose_reset_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Key", tint = Color(0xFF8E8E8E))
                }

                IconButton(
                    onClick = onTransposeUp,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                        .testTag("transpose_up_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Transpose Up", tint = Color.White)
                }
            }
        }
    }
}

// ==========================================
// 7. CHORD GRID UI
// ==========================================

@Composable
fun ChordGrid(
    chords: List<ChordTimestamp>,
    activeIndex: Int,
    transposeOffset: Int,
    gridState: LazyGridState,
    onChordSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        itemsIndexed(chords) { index, item ->
            val isActive = index == activeIndex
            val transposedChord = ChordTransposer.transpose(item.chord, transposeOffset)

            ChordTile(
                chord = transposedChord,
                timeSec = item.timeSec,
                isActive = isActive,
                onClick = { onChordSelected(item.timeSec) }
            )
        }
    }
}

@Composable
fun ChordTile(
    chord: String,
    timeSec: Float,
    isActive: Boolean,
    onClick: () -> Unit = {}
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF00FF88) else Color(0xFF252525),
        label = "bgColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.Black else Color.White,
        label = "textColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1.0f,
        label = "scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) Color.White else Color(0xFF333333),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Active Indicator Badge (Live Dot)
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = chord,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format("%.1fs", timeSec),
                fontSize = 10.sp,
                color = if (isActive) Color.Black.copy(alpha = 0.8f) else Color(0xFF8E8E8E),
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ==========================================
// 8. IMMERSIVE BOTTOM CONTROLS (MUSIC PLAYER STYLE)
// ==========================================

@Composable
fun ImmersiveBottomControls(
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio/Equalizer icon button
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2C2C2C), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Audio Equalizer",
                    tint = Color(0xFF8E8E8E)
                )
            }

            // Center Player Action Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Chord",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Main Play/Pause Fab
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF88))
                        .clickable { onPlayPauseToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Chord",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Export / Download Icon Button
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x1A00FF88), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export Chords",
                    tint = Color(0xFF00FF88)
                )
            }
        }
    }
}

// ==========================================
// 9. THEME DEFINITION
// ==========================================

@Composable
fun ChordifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00FF88),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        ),
        content = content
    )
}
