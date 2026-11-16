package com.aiutil.videoplayer.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationEngineEndpointTest {

    @Test
    fun `aq key prioritizes vertex ai express endpoint then ai studio`() {
        val endpoints = resolveGeminiEndpoints("  AQ.Ab8RN6TestKey12345  ")
        assertEquals(2, endpoints.size)
        assertTrue(endpoints[0].startsWith("https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-2.5-flash:generateContent?key="))
        assertTrue(endpoints[1].startsWith("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="))
        assertTrue(endpoints[0].endsWith("key=AQ.Ab8RN6TestKey12345"))
    }

    @Test
    fun `aiza key prioritizes ai studio endpoint then vertex ai express`() {
        val endpoints = resolveGeminiEndpoints("AIzaSyTestKey12345")
        assertEquals(2, endpoints.size)
        assertTrue(endpoints[0].startsWith("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="))
        assertTrue(endpoints[1].startsWith("https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-2.5-flash:generateContent?key="))
        assertTrue(endpoints[0].endsWith("key=AIzaSyTestKey12345"))
    }

    @Test
    fun `blank key returns empty endpoint list`() {
        assertTrue(resolveGeminiEndpoints("   ").isEmpty())
    }
}
