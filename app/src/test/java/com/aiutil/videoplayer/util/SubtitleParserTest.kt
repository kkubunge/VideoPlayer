package com.aiutil.videoplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleParserTest {

    @Test
    fun `parseSrt parses timestamps and cleans html tags`() {
        val srt = """
            1
            00:00:01,500 --> 00:00:04,250
            <i>Hello world!</i>

            2
            00:01:10,000 --> 00:01:13,500
            Second line
            with break
        """.trimIndent()

        val cues = SubtitleParser.parseAuto(srt, "srt")
        assertEquals(2, cues.size)
        assertEquals(1500L, cues[0].startMs)
        assertEquals(4250L, cues[0].endMs)
        assertEquals("Hello world!", cues[0].originalText)

        assertEquals(70_000L, cues[1].startMs)
        assertEquals("Second line\nwith break", cues[1].originalText)

        val active = SubtitleParser.findActiveCue(cues, 2_000L)
        assertNotNull(active)
        assertEquals("Hello world!", active?.originalText)

        assertNull(SubtitleParser.findActiveCue(cues, 50_000L))
    }

    @Test
    fun `parseVtt and parseAss extract clean dialogue`() {
        val vtt = """
            WEBVTT

            00:02.500 --> 00:05.000 align:middle
            First cue
        """.trimIndent()
        val vttCues = SubtitleParser.parseAuto(vtt, "vtt")
        assertEquals(1, vttCues.size)
        assertEquals(2500L, vttCues[0].startMs)
        assertEquals("First cue", vttCues[0].originalText)

        val ass = """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:10.50,0:00:12.00,Default,,0,0,0,,{\i1}Line one\NLine two{\i0}
        """.trimIndent()
        val assCues = SubtitleParser.parseAuto(ass, "ass")
        assertEquals(1, assCues.size)
        assertEquals(10_500L, assCues[0].startMs)
        assertEquals(12_000L, assCues[0].endMs)
        assertEquals("Line one\nLine two", assCues[0].originalText)
    }

    @Test
    fun `toSrt exports translated cues cleanly`() {
        val cues = SubtitleParser.parseSrt(
            """
            1
            00:00:01,000 --> 00:00:03,000
            Good morning
            """.trimIndent()
        ).map { it.copy(translatedText = "좋은 아침입니다") }

        val exported = SubtitleParser.toSrt(cues, useTranslated = true)
        assertTrue(exported.contains("00:00:01,000 --> 00:00:03,000"))
        assertTrue(exported.contains("좋은 아침입니다"))
    }
}
