package com.example.music

import com.example.model.ChordTimestamp
import kotlin.math.max

class TempoAlignmentService {

    /**
     * Menyesuaikan grid chord secara dinamis sepanjang durasi asli lagu tanpa batasan birama.
     */
    fun alignChordsToTempo(
        rawChords: List<ChordTimestamp>,
        durationSec: Float,
        bpm: Int = 120
    ): List<ChordTimestamp> {
        if (durationSec <= 0f) return rawChords

        // Hitung durasi 1 birama (4 ketukan) dalam detik
        val barDurationSec = (60f / bpm) * 4f
        
        // Hitung total birama dinamis berdasarkan durasi asli lagu (TANPA DIBATASI 64)
        val totalBars = kotlin.math.ceil(durationSec / barDurationSec).toInt().coerceAtLeast(1)

        val alignedList = mutableListOf<ChordTimestamp>()
        val chordPool = if (rawChords.isNotEmpty()) rawChords.map { it.chord } else listOf("C", "G", "Am", "F")

        for (barIndex in 0 until totalBars) {
            val timestamp = barIndex * barDurationSec
            val chord = chordPool[barIndex % chordPool.size]
            alignedList.add(ChordTimestamp(barIndex, chord, timestamp))
        }

        return alignedList
    }

    companion object {
        fun getBarDurationSec(bpm: Int, beatsPerBar: Int = 4): Float {
            val safeBpm = max(30, bpm)
            return (60.0f / safeBpm) * beatsPerBar
        }

        fun getBeatDurationSec(bpm: Int): Float {
            val safeBpm = max(30, bpm)
            return 60.0f / safeBpm
        }

        fun alignChordsToTempo(
            rawChords: List<ChordTimestamp>,
            durationSec: Float,
            bpm: Int = 120
        ): List<ChordTimestamp> = TempoAlignmentService().alignChordsToTempo(rawChords, durationSec, bpm)

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

        fun findActiveChord(
            currentTimeSec: Float,
            chords: List<ChordTimestamp>
        ): ChordTimestamp? {
            if (chords.isEmpty()) return null
            return chords.lastOrNull { it.timeSec <= currentTimeSec } ?: chords.firstOrNull()
        }
    }
}
