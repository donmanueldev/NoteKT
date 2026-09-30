package com.manuelduarte077.notyapp.features.notes.presentation.detail.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class VoiceInputResultTest {
    @Test
    fun `legacy recognition retries the next preferred Spanish locale`() {
        assertEquals(
            "es-US",
            nextUntriedLanguage(
                preferredLanguageTags = listOf("es-NI", "es-US", "es-ES"),
                attemptedLanguageTags = setOf("es-NI"),
            ),
        )
        assertNull(
            nextUntriedLanguage(
                preferredLanguageTags = listOf("es-NI", "es-US"),
                attemptedLanguageTags = setOf("es-NI", "es-US"),
            ),
        )
    }

    @Test
    fun `Spanish locale prioritizes Nicaragua and removes duplicates`() {
        assertEquals(
            listOf("es-NI", "es-US", "es-ES", "en-US", "en-GB"),
            preferredSpeechLanguageTags(Locale.forLanguageTag("es-NI")),
        )
    }

    @Test
    fun `system English remains a fallback after Spanish recognition models`() {
        assertEquals(
            listOf("en-US", "es-NI", "es-US", "es-ES", "en-GB"),
            preferredSpeechLanguageTags(Locale.forLanguageTag("en-US")),
        )
    }

    @Test
    fun `language support prefers exact Nicaragua model`() {
        assertEquals(
            "es-NI",
            selectSupportedLanguage(
                preferredLanguageTags = listOf("es-NI", "es-US", "es-ES"),
                supportedLanguageTags = listOf("en-US", "es-US", "es_NI"),
            ),
        )
    }

    @Test
    fun `language support falls back to another Spanish model`() {
        assertEquals(
            "es-US",
            selectSupportedLanguage(
                preferredLanguageTags = listOf("es-NI", "es-US"),
                supportedLanguageTags = listOf("en-US", "es-US"),
            ),
        )
        assertNull(
            selectSupportedLanguage(
                preferredLanguageTags = listOf("es-NI"),
                supportedLanguageTags = listOf("en-US"),
            ),
        )
    }

    @Test
    fun `recognition language prefers installed model over online model`() {
        assertEquals(
            "es-ES",
            selectRecognitionLanguage(
                preferredLanguageTags = listOf("es-NI", "es-ES"),
                installedLanguageTags = listOf("es-ES"),
                onlineLanguageTags = listOf("es-NI"),
            ),
        )
    }
}
