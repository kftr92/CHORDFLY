package com.example.chordfly.music

object ChordTransposer {
    private val sharpNotes = listOf(
        "C", "C#", "D", "D#", "E", "F",
        "F#", "G", "G#", "A", "A#", "B"
    )

    private val flatToSharp = mapOf(
        "Db" to "C#", "Eb" to "D#", "Gb" to "F#",
        "Ab" to "G#", "Bb" to "A#"
    )

    fun transpose(chord: String, semitones: Int): String {
        if (chord.isBlank() || semitones == 0) return chord

        val rootLength = when {
            chord.length >= 2 && (chord[1] == '#' || chord[1] == 'b') -> 2
            else -> 1
        }

        val root = chord.take(rootLength)
        val normalized = flatToSharp[root] ?: root
        val index = sharpNotes.indexOf(normalized)
        if (index < 0) return chord

        val shifted = (index + semitones).mod(12)
        return sharpNotes[shifted] + chord.drop(rootLength)
    }
}
