package com.example.music

import com.example.model.ChordTimestamp
import kotlin.math.abs

class ChordDetector {

    private val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Menghasilkan progresi chord harmonis yang dinamis mengikuti total durasi lagu.
     */
    fun detectChordsForSong(songIdOrTitle: String, durationSec: Float = 210f, bpm: Int = 120): List<ChordTimestamp> {
        val hash = abs(songIdOrTitle.hashCode())
        val rootIndex = hash % noteNames.size
        val rootNote = noteNames[rootIndex]
        val isMajor = (hash % 2 == 0)

        // Tangga nada utama
        val chordProgression = if (isMajor) {
            listOf(
                rootNote,
                noteNames[(rootIndex + 7) % 12],
                noteNames[(rootIndex + 9) % 12] + "m",
                noteNames[(rootIndex + 5) % 12]
            )
        } else {
            listOf(
                rootNote + "m",
                noteNames[(rootIndex + 8) % 12],
                noteNames[(rootIndex + 3) % 12],
                noteNames[(rootIndex + 10) % 12]
            )
        }

        val barDurationSec = (60f / bpm) * 4f
        val totalBars = kotlin.math.ceil(durationSec / barDurationSec).toInt().coerceAtLeast(1)
        val result = mutableListOf<ChordTimestamp>()

        for (i in 0 until totalBars) {
            val currentTime = i * barDurationSec
            val currentChord = chordProgression[i % chordProgression.size]
            result.add(ChordTimestamp(i, currentChord, currentTime))
        }

        return result
    }
}
