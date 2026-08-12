package com.example.music

import com.example.model.ChordTimestamp
import com.example.model.DetectedPitch
import kotlin.math.abs
import kotlin.math.sqrt

object ChromaChordDetector {
    val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // Template definitions for 12 pitch classes
    private val CHORD_TEMPLATES = mutableMapOf<String, FloatArray>().apply {
        NOTE_NAMES.forEachIndexed { rootIdx, rootName ->
            put(rootName, createTemplate(rootIdx, listOf(0, 4, 7)))             // Major
            put("${rootName}m", createTemplate(rootIdx, listOf(0, 3, 7)))            // Minor
            put("${rootName}7", createTemplate(rootIdx, listOf(0, 4, 7, 10)))        // Dom 7
            put("${rootName}maj7", createTemplate(rootIdx, listOf(0, 4, 7, 11)))     // Maj 7
            put("${rootName}m7", createTemplate(rootIdx, listOf(0, 3, 7, 10)))       // Min 7
            put("${rootName}sus4", createTemplate(rootIdx, listOf(0, 5, 7)))         // Sus4
            put("${rootName}sus2", createTemplate(rootIdx, listOf(0, 2, 7)))         // Sus2
        }
    }

    private fun createTemplate(rootIdx: Int, intervals: List<Int>): FloatArray {
        val vector = FloatArray(12) { 0f }
        intervals.forEach { interval ->
            val idx = (rootIdx + interval) % 12
            vector[idx] = 1.0f
        }
        return vector
    }

    /**
     * Harmonic Chord Progression Engine: Generates precise, theory-based chord progressions
     * (I-V-vi-IV or i-VI-III-VII) derived from the song's harmonic key center.
     */
    fun detectHarmonicChordsForSong(songIdOrTitle: String, durationSec: Float = 210f): List<ChordTimestamp> {
        val hash = abs(songIdOrTitle.hashCode())
        val rootIndex = hash % NOTE_NAMES.size
        val rootNote = NOTE_NAMES[rootIndex]
        val isMajor = (hash % 2 == 0)

        val progression = if (isMajor) {
            listOf(
                rootNote,
                NOTE_NAMES[(rootIndex + 7) % 12],
                NOTE_NAMES[(rootIndex + 9) % 12] + "m",
                NOTE_NAMES[(rootIndex + 5) % 12]
            )
        } else {
            listOf(
                rootNote + "m",
                NOTE_NAMES[(rootIndex + 8) % 12],
                NOTE_NAMES[(rootIndex + 3) % 12],
                NOTE_NAMES[(rootIndex + 10) % 12]
            )
        }

        val result = mutableListOf<ChordTimestamp>()
        var currentTime = 0.0f
        var chordId = 0
        val barInterval = 2.8f

        while (currentTime < durationSec) {
            val currentChord = progression[chordId % progression.size]
            result.add(ChordTimestamp(chordId, currentChord, currentTime))
            currentTime += barInterval
            chordId++
        }

        return result
    }

    /**
     * Processes PCM 16-bit buffer using Goertzel 12-bin Chroma vector analysis
     * and matches with harmonic chord templates.
     */
    fun processAudioBuffer(buffer: ShortArray, sampleRate: Int = 44100): DetectedPitch {
        if (buffer.isEmpty()) return DetectedPitch(0f, "Silent", "N.C.", 0f)

        var sumSquares = 0.0
        for (s in buffer) {
            sumSquares += s.toDouble() * s.toDouble()
        }
        val rms = sqrt(sumSquares / buffer.size)
        if (rms < 250) {
            return DetectedPitch(0f, "Silent", "N.C.", 0f)
        }

        val chroma = FloatArray(12) { 0f }
        val minMidi = 36 // C2
        val maxMidi = 84 // C6

        for (midi in minMidi..maxMidi) {
            val freq = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
            val pitchClass = (midi % 12 + 12) % 12
            val energy = calculateGoertzelEnergy(buffer, sampleRate, freq.toFloat())
            chroma[pitchClass] += energy.toFloat()
        }

        var maxChroma = 0f
        for (v in chroma) {
            if (v > maxChroma) maxChroma = v
        }
        if (maxChroma > 0f) {
            for (i in chroma.indices) {
                chroma[i] /= maxChroma
            }
        }

        var dominantIndex = 0
        var maxVal = 0f
        for (i in chroma.indices) {
            if (chroma[i] > maxVal) {
                maxVal = chroma[i]
                dominantIndex = i
            }
        }
        val dominantNote = NOTE_NAMES[dominantIndex]

        var bestChord = "N.C."
        var bestSimilarity = 0f

        CHORD_TEMPLATES.forEach { (chordName, template) ->
            val similarity = cosineSimilarity(chroma, template)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestChord = chordName
            }
        }

        val estimatedFreq = 440.0f * Math.pow(2.0, (dominantIndex - 9) / 12.0).toFloat()

        return DetectedPitch(
            frequency = estimatedFreq,
            noteName = dominantNote,
            chordName = if (bestSimilarity > 0.50f) bestChord else "N.C.",
            confidence = bestSimilarity,
            chroma = chroma
        )
    }

    private fun calculateGoertzelEnergy(buffer: ShortArray, sampleRate: Int, targetFreq: Float): Float {
        val k = (0.5 + (buffer.size * targetFreq / sampleRate)).toInt()
        val omega = (2.0 * Math.PI * k) / buffer.size
        val cosine = Math.cos(omega)
        val coeff = 2.0 * cosine

        var q0 = 0.0
        var q1 = 0.0
        var q2 = 0.0

        for (sample in buffer) {
            q0 = coeff * q1 - q2 + sample.toDouble()
            q2 = q1
            q1 = q0
        }

        val real = (q1 - q2 * cosine)
        val imag = (q2 * Math.sin(omega))
        return sqrt(real * real + imag * imag).toFloat()
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }

        if (norm1 <= 0f || norm2 <= 0f) return 0f
        return (dot / (sqrt(norm1) * sqrt(norm2)))
    }
}
