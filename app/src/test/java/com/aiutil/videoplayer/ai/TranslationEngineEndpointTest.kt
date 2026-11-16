package com.aiutil.videoplayer.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationEngineEndpointTest {

    @Test
    fun `resolveGeminiEndpoints prioritizes latest AI Studio models then fallbacks`() {
        val endpoints = resolveGeminiEndpoints("  AQ.TestKey12345  ")
        assertEquals(4, endpoints.size)
        assertTrue(
            endpoints[0].startsWith(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key="
            )
        )
        assertTrue(
            endpoints[1].startsWith(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-4-hydrogen:generateContent?key="
            )
        )
        assertTrue(
            endpoints[2].startsWith(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
            )
        )
        assertTrue(
            endpoints[3].startsWith(
                "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.5-flash-lite:generateContent?key="
            )
        )
        assertTrue(endpoints[0].endsWith("key=AQ.TestKey12345"))
    }

    @Test
    fun `aiza key uses same latest model order`() {
        val endpoints = resolveGeminiEndpoints("AIzaSyTestKey12345")
        assertEquals(4, endpoints.size)
        assertTrue(endpoints[0].contains("generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite"))
        assertTrue(endpoints[0].endsWith("key=AIzaSyTestKey12345"))
    }

    @Test
    fun `blank key returns empty endpoint list`() {
        assertTrue(resolveGeminiEndpoints("   ").isEmpty())
    }

    @Test
    fun `parseTranscribedSubtitles parses milliseconds response accurately`() {
        val raw = """
            [
              {"startMs": 1200, "endMs": 4500, "original": "Hello world", "translated": "안녕하세요 세상"},
              {"startMs": 5000, "endMs": 7800, "original": "Good bye", "translated": "안녕히 가세요"}
            ]
        """.trimIndent()
        val list = parseTranscribedSubtitles(raw, clipDurationMs = 8000L)
        assertEquals(2, list.size)
        assertEquals(1200L, list[0].startOffsetMs)
        assertEquals(4500L, list[0].endOffsetMs)
        assertEquals("Hello world", list[0].originalText)
        assertEquals("안녕하세요 세상", list[0].translatedText)
        assertEquals(5000L, list[1].startOffsetMs)
        assertEquals(7800L, list[1].endOffsetMs)
    }

    @Test
    fun `parseTranscribedSubtitles auto converts seconds to milliseconds`() {
        val raw = """
            [
              {"startMs": 1.2, "endMs": 4.5, "original": "One two", "translated": "하나 둘"},
              {"startMs": 5, "endMs": 7.5, "original": "Three", "translated": "셋"}
            ]
        """.trimIndent()
        val list = parseTranscribedSubtitles(raw, clipDurationMs = 8000L)
        assertEquals(2, list.size)
        assertEquals(1200L, list[0].startOffsetMs)
        assertEquals(4500L, list[0].endOffsetMs)
        assertEquals(5000L, list[1].startOffsetMs)
        assertEquals(7500L, list[1].endOffsetMs)
    }

    @Test
    fun `parseTranscribedSubtitles clamps out of range timestamps`() {
        val raw = """
            [
              {"startMs": -200, "endMs": 15000, "original": "Long line", "translated": "긴 문장"}
            ]
        """.trimIndent()
        val list = parseTranscribedSubtitles(raw, clipDurationMs = 8000L)
        assertEquals(1, list.size)
        assertEquals(0L, list[0].startOffsetMs)
        assertEquals(8000L, list[0].endOffsetMs)
    }
}
