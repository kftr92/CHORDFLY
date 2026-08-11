package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiChordAnalyzer
import com.example.audio.AudioChordEngine
import com.example.model.ChordFlyUiState
import com.example.model.ChordTimestamp
import com.example.model.YouTubeUiState
import com.example.music.ChordParser
import com.example.music.ChordTransposer
import com.example.youtube.YouTubeUrlParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CHORDFLY_YOUTUBE"

sealed interface YouTubeUiEvent {
    data class ShowToast(val message: String) : YouTubeUiEvent
}

class MainViewModel : ViewModel() {

    private val audioEngine = AudioChordEngine()
    private val geminiAnalyzer = GeminiChordAnalyzer()

    private val defaultChords = listOf(
        ChordTimestamp(0, 0.0f, "C", 0.95f),
        ChordTimestamp(1, 3.0f, "G", 0.92f),
        ChordTimestamp(2, 6.0f, "Am", 0.90f),
        ChordTimestamp(3, 9.0f, "F", 0.94f),
        ChordTimestamp(4, 12.0f, "C", 0.95f),
        ChordTimestamp(5, 15.0f, "Em", 0.88f),
        ChordTimestamp(6, 18.0f, "Am", 0.91f),
        ChordTimestamp(7, 21.0f, "F", 0.93f),
        ChordTimestamp(8, 24.0f, "Dm", 0.87f),
        ChordTimestamp(9, 27.0f, "G7", 0.89f),
        ChordTimestamp(10, 30.0f, "Cmaj7", 0.96f),
        ChordTimestamp(11, 33.0f, "C", 0.95f)
    )

    private val _uiState = MutableStateFlow(
        ChordFlyUiState(
            chords = defaultChords
        )
    )
    val uiState: StateFlow<ChordFlyUiState> = _uiState.asStateFlow()

    private val _youtubeState = MutableStateFlow(YouTubeUiState())
    val youtubeState: StateFlow<YouTubeUiState> = _youtubeState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<YouTubeUiEvent>()
    val uiEvent: SharedFlow<YouTubeUiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            audioEngine.livePitch.collect { pitch ->
                _uiState.update { state ->
                    state.copy(livePitch = pitch)
                }
            }
        }

        viewModelScope.launch {
            audioEngine.isListening.collect { listening ->
                _uiState.update { state ->
                    state.copy(isMicListening = listening)
                }
            }
        }

        recalculateActiveState()
    }

    fun onSearchQueryChange(query: String) {
        _youtubeState.update { it.copy(searchQuery = query) }
    }

    fun onYoutubeUrlChange(url: String) {
        _youtubeState.update { it.copy(youtubeUrl = url) }
    }

    fun openYouTubeSearch() {
        val query = _youtubeState.value.searchQuery.trim()
        val urlInput = _youtubeState.value.youtubeUrl.trim()

        // If user entered a direct YouTube URL or Video ID into search field, open video directly
        val candidateUrl = if (YouTubeUrlParser.extractVideoId(query) != null) query else urlInput
        val extractedId = YouTubeUrlParser.extractVideoId(candidateUrl)

        if (extractedId != null) {
            _youtubeState.update { it.copy(youtubeUrl = candidateUrl) }
            selectYouTubeVideo(extractedId)
            return
        }

        if (query.isBlank()) {
            val errMsg = "Masukkan judul lagu terlebih dahulu."
            _youtubeState.update { it.copy(status = errMsg) }
            viewModelScope.launch {
                _uiEvent.emit(YouTubeUiEvent.ShowToast(errMsg))
            }
            return
        }

        Log.d(TAG, "SEARCH_QUERY=$query")
        _youtubeState.update {
            it.copy(
                showYouTubeSearch = true,
                status = "Mencari '$query' di YouTube..."
            )
        }
    }

    fun selectYouTubeVideo(videoId: String) {
        Log.d(TAG, "VIDEO_ID_DETECTED=$videoId")

        val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&rel=0&enablejsapi=1"
        Log.d(TAG, "PLAYER_LOAD=$embedUrl")

        val songTitle = if (_youtubeState.value.searchQuery.isNotBlank()) {
            _youtubeState.value.searchQuery
        } else {
            "YouTube Video ($videoId)"
        }

        _youtubeState.update {
            it.copy(
                selectedVideoId = videoId,
                showYouTubeSearch = false,
                status = "Video dimuat ($videoId)"
            )
        }

        _uiState.update {
            it.copy(
                activeTargetUrl = embedUrl,
                title = songTitle
            )
        }

        runGeminiChordAnalysis(songTitle)
    }

    fun closeYouTubeSearch() {
        _youtubeState.update { it.copy(showYouTubeSearch = false) }
    }

    fun openVideoFromUrl() {
        val rawInput = _youtubeState.value.youtubeUrl.ifBlank { _youtubeState.value.searchQuery }.trim()
        if (rawInput.isBlank()) {
            val errMsg = "URL YouTube tidak valid."
            _youtubeState.update { it.copy(status = errMsg) }
            viewModelScope.launch {
                _uiEvent.emit(YouTubeUiEvent.ShowToast(errMsg))
            }
            return
        }

        val videoId = YouTubeUrlParser.extractVideoId(rawInput)
        if (videoId != null) {
            selectYouTubeVideo(videoId)
        } else {
            val errMsg = "URL YouTube tidak valid."
            _youtubeState.update { it.copy(status = errMsg) }
            viewModelScope.launch {
                _uiEvent.emit(YouTubeUiEvent.ShowToast(errMsg))
            }
        }
    }

    fun pasteFromClipboard(text: String?) {
        val cleanText = text?.trim().orEmpty()
        if (cleanText.isBlank()) {
            val errMsg = "Clipboard kosong."
            _youtubeState.update { it.copy(status = errMsg) }
            viewModelScope.launch {
                _uiEvent.emit(YouTubeUiEvent.ShowToast(errMsg))
            }
            return
        }

        val videoId = YouTubeUrlParser.extractVideoId(cleanText)
        if (videoId != null) {
            _youtubeState.update {
                it.copy(
                    youtubeUrl = cleanText,
                    status = "URL ditempel dari clipboard"
                )
            }
            openVideoFromUrl()
        } else {
            val errMsg = "Clipboard tidak berisi URL YouTube."
            _youtubeState.update { it.copy(status = errMsg) }
            viewModelScope.launch {
                _uiEvent.emit(YouTubeUiEvent.ShowToast(errMsg))
            }
        }
    }

    fun updatePlaybackTime(seconds: Float) {
        _uiState.update { it.copy(currentTimeSec = seconds) }
        recalculateActiveState()
    }

    fun incrementTranspose() {
        if (_uiState.value.transposeOffset < 12) {
            _uiState.update { it.copy(transposeOffset = it.transposeOffset + 1) }
            recalculateActiveState()
        }
    }

    fun decrementTranspose() {
        if (_uiState.value.transposeOffset > -12) {
            _uiState.update { it.copy(transposeOffset = it.transposeOffset - 1) }
            recalculateActiveState()
        }
    }

    fun resetTranspose() {
        _uiState.update { it.copy(transposeOffset = 0) }
        recalculateActiveState()
    }

    fun toggleMicListening() {
        if (audioEngine.isListening.value) {
            audioEngine.stopListening()
        } else {
            audioEngine.startListening(viewModelScope)
        }
    }

    fun runGeminiChordAnalysis(query: String = _youtubeState.value.searchQuery) {
        val currentQuery = query.ifBlank { _uiState.value.title }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAiAnalyzing = true,
                    aiStatusMessage = "Gemini AI menganalisis progresi chord..."
                )
            }

            val result = geminiAnalyzer.analyzeSongChords(currentQuery, _uiState.value.chords)

            if (result.chords.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(
                        title = result.songTitle.ifBlank { state.title },
                        artist = result.artist.ifBlank { state.artist },
                        key = result.key ?: state.key,
                        bpm = result.bpm ?: state.bpm,
                        chords = result.chords,
                        isAiAnalyzing = false,
                        aiStatusMessage = result.summary
                    )
                }
                recalculateActiveState()
            } else {
                _uiState.update { state ->
                    state.copy(
                        isAiAnalyzing = false,
                        aiStatusMessage = "Ready"
                    )
                }
            }
        }
    }

    private fun recalculateActiveState() {
        _uiState.update { state ->
            val time = state.currentTimeSec
            val chords = state.chords
            val offset = state.transposeOffset

            val active = chords.lastOrNull { it.timeSec <= time } ?: chords.firstOrNull()
            val activeIndex = if (active != null) chords.indexOf(active) else -1

            val nextList = if (activeIndex != -1 && activeIndex + 1 < chords.size) {
                chords.subList(activeIndex + 1, minOf(activeIndex + 4, chords.size))
            } else {
                emptyList()
            }

            val transposedChordStr = active?.let { ChordTransposer.transpose(it.chord, offset) } ?: "N.C."
            val parsedNotes = ChordParser.parse(transposedChordStr).notes

            state.copy(
                currentChord = active,
                nextChords = nextList,
                currentChordNotes = parsedNotes
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopListening()
    }
}
