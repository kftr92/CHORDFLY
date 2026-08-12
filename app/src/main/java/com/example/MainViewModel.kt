package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiChordAnalyzer
import com.example.audio.AudioChordEngine
import com.example.model.ChordFlyUiState
import com.example.model.ChordTimestamp
import com.example.model.SearchTab
import com.example.model.SongSearchResult
import com.example.model.YouTubeUiState
import com.example.music.ChordParser
import com.example.music.ChordTransposer
import com.example.music.TempoAlignmentService
import com.example.youtube.YouTubeSearchService
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

    private val defaultChords = run {
        val pattern = listOf("C", "G", "Am", "F", "C", "Em", "F", "G", "Am", "Em", "F", "C", "Dm", "G", "C", "C")
        val rawList = List(64) { index ->
            val chordName = pattern[index % pattern.size]
            ChordTimestamp(
                id = index,
                timeSec = 0f,
                chord = chordName,
                confidence = 0.95f,
                source = "Standard Preset"
            )
        }
        TempoAlignmentService.alignProgressionToBpm(rawList, targetBpm = 120)
    }

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

        performSearch(query)
    }

    fun performSearch(query: String = _youtubeState.value.searchQuery) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        Log.d(TAG, "SEARCH_QUERY=$cleanQuery")
        _youtubeState.update {
            it.copy(
                searchQuery = cleanQuery,
                showYouTubeSearch = true,
                isSearching = true,
                searchError = null,
                status = "Mencari '$cleanQuery'..."
            )
        }

        viewModelScope.launch {
            try {
                val results = YouTubeSearchService.searchSongs(cleanQuery)
                _youtubeState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = results,
                        searchError = if (results.isEmpty()) "Lagu '$cleanQuery' tidak ditemukan" else null,
                        status = "Ditemukan ${results.size} hasil"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _youtubeState.update {
                    it.copy(
                        isSearching = false,
                        searchError = e.localizedMessage ?: "Gagal terhubung ke pencarian",
                        status = "Pencarian gagal"
                    )
                }
            }
        }
    }

    fun clearYouTubeSearch() {
        _youtubeState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                searchError = null
            )
        }
    }

    fun setSearchTab(tab: SearchTab) {
        _youtubeState.update { it.copy(selectedSearchTab = tab) }
    }

    fun selectYouTubeVideo(videoId: String, titleHint: String? = null) {
        Log.d(TAG, "VIDEO_ID_DETECTED=$videoId")

        val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&rel=0&enablejsapi=1"
        Log.d(TAG, "PLAYER_LOAD=$embedUrl")

        val songTitle = titleHint?.ifBlank { null }
            ?: if (_youtubeState.value.searchQuery.isNotBlank()) {
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

    fun changeBpm(delta: Int) {
        val currentBpm = _uiState.value.bpm ?: 120
        val newBpm = (currentBpm + delta).coerceIn(40, 240)
        if (newBpm == currentBpm) return

        _uiState.update { state ->
            val reAlignedChords = TempoAlignmentService.alignProgressionToBpm(state.chords, newBpm)
            state.copy(
                bpm = newBpm,
                chords = reAlignedChords
            )
        }
        recalculateActiveState()
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
                val detectedBpm = result.bpm ?: _uiState.value.bpm ?: 120
                val alignedChords = TempoAlignmentService.alignProgressionToBpm(result.chords, detectedBpm)
                _uiState.update { state ->
                    state.copy(
                        title = result.songTitle.ifBlank { state.title },
                        artist = result.artist.ifBlank { state.artist },
                        key = result.key ?: state.key,
                        bpm = detectedBpm,
                        chords = alignedChords,
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

            val active = TempoAlignmentService.findActiveChord(time, chords)
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
