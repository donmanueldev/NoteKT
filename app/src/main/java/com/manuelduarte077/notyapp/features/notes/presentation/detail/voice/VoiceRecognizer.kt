package com.manuelduarte077.notyapp.features.notes.presentation.detail.voice

import android.content.Context

internal data class VoiceRecognitionRequest(
    val preferredLanguageTags: List<String>,
    val biasingStrings: List<String>,
)

internal sealed interface VoiceRecognitionEvent {
    data object Ready : VoiceRecognitionEvent
    data object SpeechStarted : VoiceRecognitionEvent
    data class AudioLevelChanged(val rmsDb: Float) : VoiceRecognitionEvent
    data class PartialResult(val text: String) : VoiceRecognitionEvent
    data object Processing : VoiceRecognitionEvent
    data class FinalResult(
        val candidates: List<VoiceRecognitionCandidate>,
    ) : VoiceRecognitionEvent

    data object Empty : VoiceRecognitionEvent
    data object Failed : VoiceRecognitionEvent
    data object LanguageUnavailable : VoiceRecognitionEvent
    data object ModelDownloadRequired : VoiceRecognitionEvent
    data object Unavailable : VoiceRecognitionEvent
}

internal fun interface VoiceRecognitionEventListener {
    fun onEvent(event: VoiceRecognitionEvent)
}

internal interface VoiceRecognizer {
    fun start(
        request: VoiceRecognitionRequest,
        listener: VoiceRecognitionEventListener,
    )

    fun stop()
    fun cancel()
    fun close()
}

internal fun interface VoiceRecognizerFactory {
    fun create(context: Context): VoiceRecognizer
}

internal object AndroidVoiceRecognizerFactory : VoiceRecognizerFactory {
    override fun create(context: Context): VoiceRecognizer = AndroidVoiceRecognizer(context)
}

internal sealed interface VoiceRecognitionState {
    data object Idle : VoiceRecognitionState
    data class Listening(
        val partialText: String = "",
        val rmsDb: Float? = null,
    ) : VoiceRecognitionState

    data class Processing(val partialText: String = "") : VoiceRecognitionState
}
