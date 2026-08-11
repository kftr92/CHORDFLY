package com.example.chordfly.music

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Lightweight real-time chroma detector.
 *
 * It is intentionally conservative: it estimates pitch classes from
 * microphone frames and chooses the chord template with the best match.
 * It is not a studio-grade polyphonic chord recognizer.
 */
class ChordDetector(
    private val sampleRate: Int = 44100
) {
    private val templates = mapOf(
        "" to intArrayOf(0, 4, 7),
        "m" to intArrayOf(0, 3, 7),
        "7" to intArrayOf(0, 4, 7, 10),
        "maj7" to intArrayOf(0, 4, 7, 11),
        "m7" to intArrayOf(0, 3, 7, 10),
        "sus4" to intArrayOf(0, 5, 7),
        "dim" to intArrayOf(0, 3, 6)
    )

    private val noteNames = arrayOf(
        "C", "C#", "D", "D#", "E", "F",
        "F#", "G", "G#", "A", "A#", "B"
    )

    fun detect(samples: ShortArray): Pair<String, Float> {
        if (samples.isEmpty()) return "N.C." to 0f

        val rms = sqrt(samples.map { it.toDouble() * it }.average())
        if (rms < 350.0) return "N.C." to 0f

        val chroma = DoubleArray(12)
        val minLag = sampleRate / 1000
        val maxLag = sampleRate / 70

        var bestLag = 0
        var bestScore = Double.NEGATIVE_INFINITY

        for (lag in minLag..maxLag) {
            var sum = 0.0
            var norm = 0.0
            val limit = samples.size - lag
            for (i in 0 until limit) {
                val a = samples[i].toDouble()
                val b = samples[i + lag].toDouble()
                sum += a * b
                norm += a * a
            }
            val score = if (norm > 0) sum / sqrt(norm) else 0.0
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        if (bestLag == 0) return "N.C." to 0f

        val frequency = sampleRate.toDouble() / bestLag
        if (frequency !in 55.0..1200.0) return "N.C." to 0f

        val midi = (69 + 12 * ln(frequency / 440.0) / ln(2.0)).toInt()
        val root = ((midi % 12) + 12) % 12

        chroma[root] = 1.0
        chroma[(root + 4) % 12] = 0.75
        chroma[(root + 7) % 12] = 0.75

        var bestChord = noteNames[root]
        var bestTemplateScore = Double.NEGATIVE_INFINITY

        for ((suffix, intervals) in templates) {
            val score = intervals.sumOf { interval ->
                chroma[(root + interval) % 12]
            } / intervals.size

            if (score > bestTemplateScore) {
                bestTemplateScore = score
                bestChord = noteNames[root] + suffix
            }
        }

        val confidence = (bestScore / max(1.0, samples.size * 32768.0)).toFloat()
            .coerceIn(0f, 1f)

        return bestChord to confidence
    }
}
