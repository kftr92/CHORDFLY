package com.example.music

import com.example.model.DetectedPitch
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

object ChordDetector {
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Estimates pitch and chord from 16-bit PCM buffer using normalized autocorrelation.
     */
    fun processAudioBuffer(buffer: ShortArray, sampleRate: Int = 44100): DetectedPitch {
        if (buffer.isEmpty()) return DetectedPitch(0f, "Silent", "None", 0f)

        // Calculate RMS Energy to check for silence
        var sumSquares = 0.0
        for (sample in buffer) {
            sumSquares += sample * sample
        }
        val rms = Math.sqrt(sumSquares / buffer.size)
        if (rms < 300) { // Silence threshold
            return DetectedPitch(0f, "Silent", "None", 0f)
        }

        // Autocorrelation Pitch Detection
        val minLag = sampleRate / 1000 // Max ~1000 Hz
        val maxLag = sampleRate / 60   // Min ~60 Hz (Bass note)
        var maxCorrelation = 0.0
        var bestLag = -1

        for (lag in minLag..Math.min(maxLag, buffer.size - 1)) {
            var correlation = 0.0
            for (i in 0 until buffer.size - lag) {
                correlation += buffer[i].toDouble() * buffer[i + lag].toDouble()
            }
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestLag = lag
            }
        }

        if (bestLag <= 0) return DetectedPitch(0f, "Unclear", "N.C.", 0f)

        val frequency = sampleRate.toFloat() / bestLag
        val noteName = frequencyToNote(frequency)
        val chordEstimate = noteToTriadChord(noteName)
        val confidence = Math.min(1.0f, (maxCorrelation / (sumSquares + 1.0)).toFloat() * 1.5f)

        return DetectedPitch(frequency, noteName, chordEstimate, confidence)
    }

    fun frequencyToNote(freq: Float): String {
        if (freq <= 0) return "-"
        val midiNumber = (69 + 12 * log2(freq / 440.0)).roundToInt()
        val noteIndex = (midiNumber % 12 + 12) % 12
        val octave = (midiNumber / 12) - 1
        return "${NOTE_NAMES[noteIndex]}$octave"
    }

    private fun noteToTriadChord(noteWithOctave: String): String {
        val root = noteWithOctave.replace(Regex("[0-9-]"), "")
        if (root.isBlank() || root == "Silent" || root == "Unclear") return "N.C."
        return root // Default root triad major
    }
}
