package com.example

import com.example.model.ChordTimestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class ChordTimelineTest {

    private val timeline = listOf(
        ChordTimestamp(0, 0.0f, "C"),
        ChordTimestamp(1, 3.2f, "G"),
        ChordTimestamp(2, 6.1f, "Am"),
        ChordTimestamp(3, 9.0f, "F")
    )

    @Test
    fun testActiveChordByTimestamp() {
        var time = 0.5f
        var active = timeline.lastOrNull { it.timeSec <= time }
        assertEquals("C", active?.chord)

        time = 4.0f
        active = timeline.lastOrNull { it.timeSec <= time }
        assertEquals("G", active?.chord)

        time = 7.0f
        active = timeline.lastOrNull { it.timeSec <= time }
        assertEquals("Am", active?.chord)

        time = 9.5f
        active = timeline.lastOrNull { it.timeSec <= time }
        assertEquals("F", active?.chord)
    }
}
