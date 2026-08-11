package com.example.music

object ChordTransposer {
    private val NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun transpose(chord: String, semitones: Int): String {
        if (semitones == 0 || chord.isBlank() || chord == "N.C." || chord == "None") return chord

        if (chord.contains("/")) {
            val parts = chord.split("/")
            val main = transposeSingleNote(parts[0], semitones)
            val bass = transposeSingleNote(parts[1], semitones)
            return "$main/$bass"
        }

        return transposeSingleNote(chord, semitones)
    }

    private fun transposeSingleNote(chord: String, semitones: Int): String {
        val baseNote = when {
            chord.length >= 2 && (chord[1] == '#' || chord[1] == 'b') -> chord.substring(0, 2)
            chord.isNotEmpty() -> chord.substring(0, 1)
            else -> return chord
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
