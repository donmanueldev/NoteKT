package com.manuelduarte077.notyapp.features.notes.presentation.detail.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class VoiceInputResultTest {
    @Test
    fun `recognition retries only configured locales`() {
        assertEquals(
            "fr-FR",
            nextUntriedLanguage(
                preferredLanguageTags = listOf("de-DE", "fr-FR"),
                attemptedLanguageTags = setOf("de-DE"),
            ),
        )
        assertNull(
            nextUntriedLanguage(
                preferredLanguageTags = listOf("fr-FR"),
                attemptedLanguageTags = setOf("fr-FR"),
            ),
        )
    }

    @Test
    fun `only the device locale is requested`() {
        assertEquals(
            listOf("es-NI"),
            preferredSpeechLanguageTags(Locale.forLanguageTag("es-NI")),
        )
        assertEquals(
            listOf("fr-FR"),
            preferredSpeechLanguageTags(Locale.forLanguageTag("fr-FR")),
        )
    }

    @Test
    fun `language support prefers the exact device locale`() {
        assertEquals(
            "fr-FR",
            selectSupportedLanguage(
                preferredLanguageTags = listOf("fr-FR"),
                supportedLanguageTags = listOf("en-US", "fr-CA", "fr_FR"),
            ),
        )
    }

    @Test
    fun `language support can use another region for the device language`() {
        assertEquals(
            "fr-CA",
            selectSupportedLanguage(
                preferredLanguageTags = listOf("fr-FR"),
                supportedLanguageTags = listOf("en-US", "fr-CA"),
            ),
        )
        assertNull(
            selectSupportedLanguage(
                preferredLanguageTags = listOf("fr-FR"),
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
