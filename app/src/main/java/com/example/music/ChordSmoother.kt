package com.example.music

import com.example.model.DetectedPitch

class ChordSmoother(
    private val windowSize: Int = 5,
    private val minConfidenceThreshold: Float = 0.50f
) {
    private val buffer = ArrayDeque<DetectedPitch>()
    private var lastStableChord: String = "N.C."

    fun smooth(rawPitch: DetectedPitch): DetectedPitch {
        if (rawPitch.confidence < minConfidenceThreshold) {
            return rawPitch.copy(chordName = lastStableChord)
        }

        buffer.addLast(rawPitch)
        if (buffer.size > windowSize) {
            buffer.removeFirst()
        }

        // Count occurrences of candidate chords in current buffer window
        val frequencyMap = mutableMapOf<String, Float>()
        for (pitch in buffer) {
            val weight = pitch.confidence
            frequencyMap[pitch.chordName] = (frequencyMap[pitch.chordName] ?: 0f) + weight
        }

        var bestCandidate = lastStableChord
        var maxWeight = 0f

        frequencyMap.forEach { (chord, totalWeight) ->
            if (totalWeight > maxWeight) {
                maxWeight = totalWeight
                bestCandidate = chord
            }
        }

        lastStableChord = bestCandidate
        return rawPitch.copy(chordName = bestCandidate)
    }

    fun reset() {
        buffer.clear()
        lastStableChord = "N.C."
    }
}
