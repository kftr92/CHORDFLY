package com.example.chordfly.model

data class ChordTimestamp(
    val id: Long,
    val chord: String,
    val timeSec: Float,
    val confidence: Float = 0f,
    val source: ChordSource = ChordSource.DSP
)

enum class ChordSource {
    DSP, GEMINI, MANUAL
}

data class ChordAnalysis(
    val title: String = "",
    val artist: String = "",
    val key: String? = null,
    val bpm: Int? = null,
    val chords: List<ChordTimestamp> = emptyList()
)
