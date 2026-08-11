package com.example

import com.example.music.ChordParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordParserTest {

    @Test
    fun testParseMajorTriad() {
        val parsed = ChordParser.parse("C")
        assertEquals("C", parsed.root)
        assertEquals("", parsed.quality)
        assertEquals(listOf("C", "E", "G"), parsed.notes)
    }

    @Test
    fun testParseMinorTriad() {
        val parsed = ChordParser.parse("Am")
        assertEquals("A", parsed.root)
        assertEquals("m", parsed.quality)
        assertEquals(listOf("A", "C", "E"), parsed.notes)
    }

    @Test
    fun testParseSeventhChords() {
        val maj7 = ChordParser.parse("Cmaj7")
        assertEquals(listOf("C", "E", "G", "B"), maj7.notes)

        val g7 = ChordParser.parse("G7")
        assertEquals(listOf("G", "B", "D", "F"), g7.notes)
    }

    @Test
    fun testParseSlashChord() {
        val parsed = ChordParser.parse("C/E")
        assertEquals("C", parsed.root)
        assertEquals("E", parsed.bassNote)
        assertTrue(parsed.notes.isNotEmpty())
        assertEquals("E", parsed.notes.first())
    }
}
