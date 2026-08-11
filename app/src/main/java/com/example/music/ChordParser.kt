package com.example.music

import com.example.model.ParsedChord

object ChordParser {
    private val NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun parse(chordStr: String): ParsedChord {
        val clean = chordStr.trim()
        if (clean.isBlank() || clean == "N.C." || clean == "None") {
            return ParsedChord(rawChord = clean, root = "N.C.", quality = "", notes = emptyList())
        }

        var mainPart = clean
        var bassNote: String? = null

        if (clean.contains("/")) {
            val parts = clean.split("/")
            mainPart = parts[0]
            bassNote = parts.getOrNull(1)
        }

        val root = extractRootNote(mainPart)
        val quality = mainPart.removePrefix(root)

        val notes = calculateNotes(root, quality, bassNote)

        return ParsedChord(
            rawChord = clean,
            root = root,
            quality = quality,
            bassNote = bassNote,
            notes = notes
        )
    }

    private fun extractRootNote(chord: String): String {
        return when {
            chord.length >= 2 && (chord[1] == '#' || chord[1] == 'b') -> normalizeNote(chord.substring(0, 2))
            chord.isNotEmpty() -> normalizeNote(chord.substring(0, 1))
            else -> "C"
        }
    }

    private fun normalizeNote(note: String): String {
        return when (note) {
            "Db" -> "C#"
            "Eb" -> "D#"
            "Gb" -> "F#"
            "Ab" -> "G#"
            "Bb" -> "A#"
            else -> note
        }
    }

    private fun calculateNotes(root: String, quality: String, bassNote: String?): List<String> {
        val rootIndex = NOTES.indexOf(root)
        if (rootIndex == -1) return emptyList()

        // Intervals in semitones relative to root
        val intervals = when {
            quality == "m" || quality == "min" || quality == "-" -> listOf(0, 3, 7) // Minor
            quality == "m7" || quality == "min7" -> listOf(0, 3, 7, 10) // Minor 7
            quality == "7" || quality == "dom7" -> listOf(0, 4, 7, 10) // Dominant 7
            quality == "maj7" || quality == "M7" -> listOf(0, 4, 7, 11) // Major 7
            quality == "sus4" -> listOf(0, 5, 7) // Suspended 4
            quality == "sus2" -> listOf(0, 2, 7) // Suspended 2
            quality == "dim" || quality == "o" -> listOf(0, 3, 6) // Diminished
            quality == "aug" || quality == "+" -> listOf(0, 4, 8) // Augmented
            quality == "6" -> listOf(0, 4, 7, 9) // Major 6
            quality == "m6" -> listOf(0, 3, 7, 9) // Minor 6
            quality == "9" -> listOf(0, 4, 7, 10, 14) // Dominant 9
            else -> listOf(0, 4, 7) // Default Major triad
        }

        val resultNotes = mutableListOf<String>()

        bassNote?.let {
            val normBass = normalizeNote(it)
            if (NOTES.contains(normBass)) {
                resultNotes.add(normBass)
            }
        }

        for (interval in intervals) {
            val noteIdx = (rootIndex + interval) % NOTES.size
            val noteName = NOTES[noteIdx]
            if (!resultNotes.contains(noteName)) {
                resultNotes.add(noteName)
            }
        }

        return resultNotes
    }
}
