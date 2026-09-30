package com.manuelduarte077.notyapp.features.notes.presentation.detail.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceRecognitionCandidateTest {
    @Test
    fun `filters blank candidates keeps confidence alignment and ranks strongest first`() {
        assertEquals(
            listOf(
                VoiceRecognitionCandidate("Comprar leche", 0.91f),
                VoiceRecognitionCandidate("Comprar lecho", 0.42f),
            ),
            rankVoiceCandidates(
                texts = listOf(" ", " Comprar lecho ", "Comprar leche"),
                confidenceScores = floatArrayOf(0.99f, 0.42f, 0.91f),
            ),
        )
    }

    @Test
    fun `preserves recognizer order when confidence is unavailable and limits results`() {
        assertEquals(
            (1..5).map { VoiceRecognitionCandidate("Opción $it", null) },
            rankVoiceCandidates(
                texts = (1..7).map { "Opción $it" },
                confidenceScores = null,
            ),
        )
    }
}
