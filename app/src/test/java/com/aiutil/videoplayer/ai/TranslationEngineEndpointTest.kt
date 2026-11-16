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
}
