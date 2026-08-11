package com.example.chordfly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chordfly.ai.GeminiChordAnalyzer
import com.example.chordfly.model.ChordAnalysis
import com.example.chordfly.model.ChordSource
import com.example.chordfly.model.ChordTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChordFlyUiState(
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val transpose: Int = 0,
    val currentTime: Float = 0f,
    val activeIndex: Int = -1,
    val chords: List<ChordTimestamp> = emptyList(),
    val key: String? = null,
    val bpm: Int? = null,
    val isAnalyzing: Boolean = false,
    val error: String? = null
)

class MainViewModel : ViewModel() {
    private val analyzer = GeminiChordAnalyzer()

    private val _ui = MutableStateFlow(ChordFlyUiState())
    val ui: StateFlow<ChordFlyUiState> = _ui.asStateFlow()

    fun setVideoId(id: String) {
        _ui.value = _ui.value.copy(videoId = id)
    }

    fun setTitle(title: String) {
        _ui.value = _ui.value.copy(title = title)
    }

    fun setArtist(artist: String) {
        _ui.value = _ui.value.copy(artist = artist)
    }

    fun setTime(seconds: Float) {
        val list = _ui.value.chords
        val index = list.indexOfLast { it.timeSec <= seconds }
        _ui.value = _ui.value.copy(currentTime = seconds, activeIndex = index)
    }

    fun addDspChord(chord: ChordTimestamp) {
        val next = _ui.value.chords + chord.copy(
            id = System.nanoTime(),
            timeSec = _ui.value.currentTime
        )
        _ui.value = _ui.value.copy(chords = next.sortedBy { it.timeSec })
    }

    fun transpose(delta: Int) {
        _ui.value = _ui.value.copy(
            transpose = (_ui.value.transpose + delta).coerceIn(-12, 12)
        )
    }

    fun clearTranspose() {
        _ui.value = _ui.value.copy(transpose = 0)
    }

    fun analyzeWithGemini() {
        val state = _ui.value
        if (state.chords.isEmpty()) {
            _ui.value = state.copy(error = "Belum ada data chord untuk dianalisis.")
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(isAnalyzing = true, error = null)
            analyzer.refine(state.title, state.artist, state.chords)
                .onSuccess { result: ChordAnalysis ->
                    _ui.value = _ui.value.copy(
                        chords = result.chords,
                        key = result.key,
                        bpm = result.bpm,
                        isAnalyzing = false
                    )
                }
                .onFailure { error ->
                    _ui.value = _ui.value.copy(
                        isAnalyzing = false,
                        error = error.message ?: "Gemini gagal menganalisis chord."
                    )
                }
        }
    }
}
