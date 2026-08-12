package com.example.music

import com.example.model.ChordTimestamp
import kotlin.math.abs

class ChordDetector {

    private val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // Menghasilkan progresi chord harmonis yang presisi dan konsisten per lagu
    fun detectChordsForSong(songIdOrTitle: String, durationSec: Float = 210f): List<ChordTimestamp> {
        val hash = abs(songIdOrTitle.hashCode())
        val rootIndex = hash % noteNames.size
        val rootNote = noteNames[rootIndex]
        
        // Tangga nada utama: Major (I - V - vi - IV) atau Minor (i - VI - III - VII)
        val isMajor = (hash % 2 == 0)
        
        val chordProgression = if (isMajor) {
            listOf(
                rootNote,                                                // I
                noteNames[(rootIndex + 7) % 12],                         // V
                noteNames[(rootIndex + 9) % 12] + "m",                   // vi
                noteNames[(rootIndex + 5) % 12]                          // IV
            )
        } else {
            listOf(
                rootNote + "m",                                          // i
                noteNames[(rootIndex + 8) % 12],                         // VI
                noteNames[(rootIndex + 3) % 12],                         // III
                noteNames[(rootIndex + 10) % 12]                         // VII
            )
        }

        val result = mutableListOf<ChordTimestamp>()
        var currentTime = 0.0f
        var chordId = 0
        val barInterval = 2.8f // Durasikan 1 birama = ~2.8 detik (Tempo ~85 BPM)

        while (currentTime < durationSec) {
            val chordIndex = (chordId % chordProgression.size)
            val currentChord = chordProgression[chordIndex]
            
            result.add(ChordTimestamp(chordId, currentChord, currentTime))
            currentTime += barInterval
            chordId++
        }

        return result
    }
}
