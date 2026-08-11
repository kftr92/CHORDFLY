package com.example

import com.example.music.ChordTransposer
import org.junit.Assert.assertEquals
import org.junit.Test

class ChordTransposerTest {

    @Test
    fun testTransposeBasic() {
        assertEquals("D", ChordTransposer.transpose("C", 2))
        assertEquals("A", ChordTransposer.transpose("G", 2))
        assertEquals("Bm", ChordTransposer.transpose("Am", 2))
        assertEquals("D#", ChordTransposer.transpose("F", -2))
        assertEquals("C", ChordTransposer.transpose("C", 0))
    }

    @Test
    fun testTransposeSlashChords() {
        assertEquals("D/F#", ChordTransposer.transpose("C/E", 2))
        assertEquals("G/B", ChordTransposer.transpose("F/A", 2))
    }

    @Test
    fun testTransposeFullLoop() {
        assertEquals("C", ChordTransposer.transpose("C", 12))
        assertEquals("C", ChordTransposer.transpose("C", -12))
    }
}
