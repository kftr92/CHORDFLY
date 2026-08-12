package com.example.music

import com.example.model.ChordTimestamp
import kotlin.math.max

/**
 * Service responsible for aligning chord progression step intervals based on song tempo (BPM).
 * Ensures visual chord highlights align with musical beats and bar measures.
 */
object TempoAlignmentService {

    /**
     * Calculates the duration of a single 4/4 bar (measure) in seconds based on BPM.
     */
    fun getBarDurationSec(bpm: Int, beatsPerBar: Int = 4): Float {
        val safeBpm = max(30, bpm)
        return (60.0f / safeBpm) * beatsPerBar
    }

    /**
     * Calculates duration of a single beat in seconds.
     */
    fun getBeatDurationSec(bpm: Int): Float {
        val safeBpm = max(30, bpm)
        return 60.0f / safeBpm
    }

    /**
     * Recalculates and aligns every chord in the list so that step intervals
     * align precisely to the target BPM bar durations.
     */
    fun alignProgressionToBpm(
        chords: List<ChordTimestamp>,
        targetBpm: Int,
        introOffsetSec: Float = 0f,
        beatsPerBar: Int = 4
    ): List<ChordTimestamp> {
        if (chords.isEmpty()) return emptyList()
        val barDuration = getBarDurationSec(targetBpm, beatsPerBar)

        return chords.mapIndexed { index, chord ->
            val alignedTime = introOffsetSec + (index * barDuration)
            chord.copy(timeSec = alignedTime)
        }
    }

    /**
     * Rescales timestamps when BPM changes dynamically.
     */
    fun rescaleTimestampsForBpm(
        chords: List<ChordTimestamp>,
        sourceBpm: Int,
        targetBpm: Int
    ): List<ChordTimestamp> {
        if (chords.isEmpty() || sourceBpm <= 0 || targetBpm <= 0 || sourceBpm == targetBpm) return chords
        val ratio = sourceBpm.toFloat() / targetBpm.toFloat()
        return chords.map { chord ->
            chord.copy(timeSec = chord.timeSec * ratio)
        }
    }

    /**
     * Returns the active bar index at [currentTimeSec] for a given BPM.
     */
    fun getActiveBarIndex(
        currentTimeSec: Float,
        bpm: Int,
        totalBars: Int,
        introOffsetSec: Float = 0f,
        beatsPerBar: Int = 4
    ): Int {
        if (totalBars <= 0 || currentTimeSec < introOffsetSec) return 0
        val barDuration = getBarDurationSec(bpm, beatsPerBar)
        val elapsed = currentTimeSec - introOffsetSec
        val barIndex = (elapsed / barDuration).toInt()
        return barIndex.coerceIn(0, totalBars - 1)
    }

    /**
     * Finds the active chord matching [currentTimeSec], utilizing step intervals.
     */
    fun findActiveChord(
        currentTimeSec: Float,
        chords: List<ChordTimestamp>
    ): ChordTimestamp? {
        if (chords.isEmpty()) return null
        return chords.lastOrNull { it.timeSec <= currentTimeSec } ?: chords.firstOrNull()
    }
}
