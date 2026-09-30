package com.manuelduarte077.notyapp.features.notes.presentation.detail.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import java.util.Locale

internal class AndroidVoiceRecognizer(
    context: Context,
) : VoiceRecognizer {
    private val applicationContext = context.applicationContext
    private val recognizer: SpeechRecognizer? = if (
        SpeechRecognizer.isRecognitionAvailable(applicationContext)
    ) {
        SpeechRecognizer.createSpeechRecognizer(applicationContext)
    } else {
        null
    }

    private var closed = false
    private var sessionSequence = 0L
    private var activeSession: Session? = null

    override fun start(
        request: VoiceRecognitionRequest,
        listener: VoiceRecognitionEventListener,
    ) {
        if (closed) return
        val recognizer = recognizer
        if (recognizer == null) {
            listener.onEvent(VoiceRecognitionEvent.Unavailable)
            return
        }

        cancelActiveSession(recognizer)
        val preferredTags = request.preferredLanguageTags
            .map(::normalizeLanguageTag)
            .filter(String::isNotBlank)
            .distinct()
            .ifEmpty { listOf(Locale.getDefault().toLanguageTag()) }
        val session = Session(
            id = ++sessionSequence,
            request = request,
            preferredLanguageTags = preferredTags,
            listener = listener,
        )
        activeSession = session

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkRecognitionSupport(recognizer, session)
            } else {
                startNextLanguage(recognizer, session)
            }
        } catch (_: RuntimeException) {
            finish(session.id, session.activeAttempt, VoiceRecognitionEvent.Failed)
        }
    }

    override fun stop() {
        if (!closed && activeSession != null) recognizer?.stopListening()
    }

    override fun cancel() {
        if (closed) return
        recognizer?.let(::cancelActiveSession)
    }

    override fun close() {
        if (closed) return
        recognizer?.let(::cancelActiveSession)
        recognizer?.destroy()
        closed = true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkRecognitionSupport(recognizer: SpeechRecognizer, session: Session) {
        val fallbackTag = session.preferredLanguageTags.first()
        recognizer.checkRecognitionSupport(
            recognitionIntent(fallbackTag, session.request.biasingStrings),
            applicationContext.mainExecutor,
            object : RecognitionSupportCallback {
                override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                    if (!isCurrentSession(session.id)) return
                    val readyLanguages = recognitionSupport.installedOnDeviceLanguages +
                        recognitionSupport.onlineLanguages
                    val selected = selectSupportedLanguage(
                        session.preferredLanguageTags,
                        readyLanguages,
                    )
                    if (selected != null) {
                        startLanguage(recognizer, session, selected)
                        return
                    }

                    val downloadable = selectSupportedLanguage(
                        session.preferredLanguageTags,
                        recognitionSupport.supportedOnDeviceLanguages,
                    )
                    if (downloadable != null) {
                        try {
                            recognizer.triggerModelDownload(
                                recognitionIntent(downloadable, session.request.biasingStrings),
                            )
                            finish(
                                session.id,
                                session.activeAttempt,
                                VoiceRecognitionEvent.ModelDownloadRequired,
                            )
                        } catch (_: RuntimeException) {
                            finish(
                                session.id,
                                session.activeAttempt,
                                VoiceRecognitionEvent.Failed,
                            )
                        }
                    } else {
                        finish(
                            session.id,
                            session.activeAttempt,
                            VoiceRecognitionEvent.LanguageUnavailable,
                        )
                    }
                }

                override fun onError(error: Int) {
                    if (!isCurrentSession(session.id)) return
                    // Some OEM recognizers cannot report support even though recognition works.
                    startNextLanguage(recognizer, session)
                }
            },
        )
    }

    private fun startNextLanguage(recognizer: SpeechRecognizer, session: Session) {
        val language = nextUntriedLanguage(
            preferredLanguageTags = session.preferredLanguageTags,
            attemptedLanguageTags = session.attemptedLanguageTags,
        )
        if (language == null) {
            finish(
                session.id,
                session.activeAttempt,
                VoiceRecognitionEvent.LanguageUnavailable,
            )
        } else {
            startLanguage(recognizer, session, language)
        }
    }

    private fun startLanguage(
        recognizer: SpeechRecognizer,
        session: Session,
        languageTag: String,
    ) {
        if (!isCurrentSession(session.id)) return
        session.attemptedLanguageTags += normalizeLanguageTag(languageTag)
        val attempt = ++session.attemptSequence
        session.activeAttempt = attempt
        recognizer.setRecognitionListener(SessionRecognitionListener(session.id, attempt))
        try {
            recognizer.startListening(
                recognitionIntent(languageTag, session.request.biasingStrings),
            )
        } catch (_: RuntimeException) {
            finish(session.id, attempt, VoiceRecognitionEvent.Failed)
        }
    }

    private fun onRecognitionError(sessionId: Long, attempt: Long, error: Int) {
        if (!isCurrentAttempt(sessionId, attempt)) return
        if (
            error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
            error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
        ) {
            val session = activeSession ?: return
            val currentRecognizer = recognizer ?: return
            startNextLanguage(currentRecognizer, session)
            return
        }

        val event = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceRecognitionEvent.Empty

            else -> VoiceRecognitionEvent.Failed
        }
        finish(sessionId, attempt, event)
    }

    private fun onRecognitionResults(sessionId: Long, attempt: Long, results: Bundle?) {
        if (!isCurrentAttempt(sessionId, attempt)) return
        val candidates = rankVoiceCandidates(
            texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION),
            confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES),
        )
        finish(
            sessionId,
            attempt,
            if (candidates.isEmpty()) {
                VoiceRecognitionEvent.Empty
            } else {
                VoiceRecognitionEvent.FinalResult(candidates)
            },
        )
    }

    private fun recognitionIntent(
        languageTag: String,
        biasingStrings: List<String>,
    ) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_VOICE_RESULTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            putStringArrayListExtra(
                RecognizerIntent.EXTRA_BIASING_STRINGS,
                ArrayList(biasingStrings),
            )
            putExtra(
                RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY,
            )
        }
    }

    private fun cancelActiveSession(recognizer: SpeechRecognizer) {
        activeSession = null
        sessionSequence++
        recognizer.cancel()
    }

    private fun isCurrentSession(sessionId: Long): Boolean =
        !closed && activeSession?.id == sessionId

    private fun isCurrentAttempt(sessionId: Long, attempt: Long): Boolean =
        isCurrentSession(sessionId) && activeSession?.activeAttempt == attempt

    private fun emit(sessionId: Long, attempt: Long, event: VoiceRecognitionEvent) {
        if (isCurrentAttempt(sessionId, attempt)) activeSession?.listener?.onEvent(event)
    }

    private fun finish(sessionId: Long, attempt: Long, event: VoiceRecognitionEvent) {
        if (!isCurrentSession(sessionId)) return
        if (attempt != NO_ATTEMPT && activeSession?.activeAttempt != attempt) return
        val listener = activeSession?.listener
        activeSession = null
        listener?.onEvent(event)
    }

    private inner class SessionRecognitionListener(
        private val sessionId: Long,
        private val attempt: Long,
    ) : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) =
            emit(sessionId, attempt, VoiceRecognitionEvent.Ready)

        override fun onBeginningOfSpeech() =
            emit(sessionId, attempt, VoiceRecognitionEvent.SpeechStarted)

        override fun onRmsChanged(rmsdB: Float) =
            emit(sessionId, attempt, VoiceRecognitionEvent.AudioLevelChanged(rmsdB))

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() =
            emit(sessionId, attempt, VoiceRecognitionEvent.Processing)

        override fun onError(error: Int) = onRecognitionError(sessionId, attempt, error)

        override fun onResults(results: Bundle?) =
            onRecognitionResults(sessionId, attempt, results)

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isCurrentAttempt(sessionId, attempt)) return
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotBlank()) {
                emit(sessionId, attempt, VoiceRecognitionEvent.PartialResult(text))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private data class Session(
        val id: Long,
        val request: VoiceRecognitionRequest,
        val preferredLanguageTags: List<String>,
        val listener: VoiceRecognitionEventListener,
        val attemptedLanguageTags: MutableSet<String> = linkedSetOf(),
        var attemptSequence: Long = 0L,
        var activeAttempt: Long = NO_ATTEMPT,
    )

    private companion object {
        const val NO_ATTEMPT = -1L
    }
}

internal fun selectSupportedLanguage(
    preferredLanguageTags: List<String>,
    supportedLanguageTags: List<String>,
): String? {
    val supported = supportedLanguageTags
        .map(::normalizeLanguageTag)
        .filter(String::isNotBlank)
        .distinct()
    val preferred = preferredLanguageTags.map(::normalizeLanguageTag)

    preferred.firstNotNullOfOrNull { desired ->
        supported.firstOrNull { it.equals(desired, ignoreCase = true) }
    }?.let { return it }

    return preferred.firstNotNullOfOrNull { desired ->
        val desiredLanguage = Locale.forLanguageTag(desired).language
        supported.firstOrNull {
            Locale.forLanguageTag(it).language.equals(desiredLanguage, ignoreCase = true)
        }
    }
}

internal fun nextUntriedLanguage(
    preferredLanguageTags: List<String>,
    attemptedLanguageTags: Set<String>,
): String? {
    val attempted = attemptedLanguageTags.map(::normalizeLanguageTag).toSet()
    return preferredLanguageTags
        .map(::normalizeLanguageTag)
        .firstOrNull { it !in attempted }
}

private fun normalizeLanguageTag(tag: String): String =
    Locale.forLanguageTag(tag.replace('_', '-')).toLanguageTag()
