package com.manuelduarte077.notyapp.features.notes.presentation.detail.voice

internal data class VoiceRecognitionCandidate(
    val text: String,
    val confidence: Float?,
)

internal sealed interface VoiceInputResult {
    data class Recognized(
        val candidates: List<VoiceRecognitionCandidate>,
    ) : VoiceInputResult

    data object Cancelled : VoiceInputResult
    data object Empty : VoiceInputResult
    data object Failed : VoiceInputResult
    data object LanguageUnavailable : VoiceInputResult
    data object ModelDownloadRequired : VoiceInputResult
    data object PermissionDenied : VoiceInputResult
    data object Unavailable : VoiceInputResult
}

internal fun rankVoiceCandidates(
    texts: List<String>?,
    confidenceScores: FloatArray?,
): List<VoiceRecognitionCandidate> = texts
    .orEmpty()
    .mapIndexedNotNull { index, rawText ->
        val text = rawText.trim()
        if (text.isBlank()) {
            null
        } else {
            VoiceRecognitionCandidate(
                text = text,
                confidence = confidenceScores
                    ?.getOrNull(index)
                    ?.takeIf { it in 0f..1f },
            )
        }
    }
    .distinctBy { it.text.lowercase() }
    .let { candidates ->
        if (candidates.any { it.confidence != null }) {
            candidates.sortedWith(
                compareByDescending<VoiceRecognitionCandidate> { it.confidence != null }
                    .thenByDescending { it.confidence },
            )
        } else {
            candidates
        }
    }
    .take(MAX_VOICE_RESULTS)

internal const val MAX_VOICE_RESULTS = 5
