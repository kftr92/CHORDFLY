package com.example.model

data class ChordTimestamp(
    val id: Int,
    val timeSec: Float,
    val chord: String,
    val confidence: Float = 1.0f,
    val source: String = "Preset"
)

data class ParsedChord(
    val rawChord: String,
    val root: String,
    val quality: String,
    val bassNote: String? = null,
    val notes: List<String> = emptyList()
)

data class DetectedPitch(
    val frequency: Float,
    val noteName: String,
    val chordName: String,
    val confidence: Float,
    val chroma: FloatArray = FloatArray(12) { 0f }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DetectedPitch
        if (frequency != other.frequency) return false
        if (noteName != other.noteName) return false
        if (chordName != other.chordName) return false
        if (confidence != other.confidence) return false
        if (!chroma.contentEquals(other.chroma)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = frequency.hashCode()
        result = 31 * result + noteName.hashCode()
        result = 31 * result + chordName.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + chroma.contentHashCode()
        return result
    }
}

data class GeminiChordResult(
    val songTitle: String = "",
    val artist: String = "",
    val key: String? = null,
    val bpm: Int? = null,
    val chords: List<ChordTimestamp> = emptyList(),
    val summary: String = ""
)

data class ChordFlyUiState(
    val title: String = "CHORDFLY Demo",
    val artist: String = "Featured Track",
    val searchInput: String = "",
    val activeTargetUrl: String = "https://www.youtube-nocookie.com/embed/jfKfPfyJRdk?autoplay=1&controls=1&modestbranding=1&rel=0&enablejsapi=1",
    val currentTimeSec: Float = 0f,
    val transposeOffset: Int = 0,
    val key: String? = "C",
    val bpm: Int? = 120,
    val chords: List<ChordTimestamp> = emptyList(),
    val currentChord: ChordTimestamp? = null,
    val nextChords: List<ChordTimestamp> = emptyList(),
    val currentChordNotes: List<String> = emptyList(),
    val isMicListening: Boolean = false,
    val livePitch: DetectedPitch = DetectedPitch(0f, "Off", "N.C.", 0f),
    val isAiAnalyzing: Boolean = false,
    val aiStatusMessage: String = "Ready",
    val error: String? = null
)
