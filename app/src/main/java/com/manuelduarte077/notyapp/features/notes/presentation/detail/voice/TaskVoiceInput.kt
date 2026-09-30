package com.manuelduarte077.notyapp.features.notes.presentation.detail.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

internal class TaskVoiceInput(
    val state: VoiceRecognitionState,
    val launch: () -> Unit,
    val cancel: () -> Unit,
) {
    val isInProgress: Boolean
        get() = state !is VoiceRecognitionState.Idle
}

@Composable
internal fun rememberTaskVoiceInput(
    recognizerFactory: VoiceRecognizerFactory = AndroidVoiceRecognizerFactory,
    onResult: (VoiceInputResult) -> Unit,
): TaskVoiceInput {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResult by rememberUpdatedState(onResult)
    val recognizer = remember(context.applicationContext, recognizerFactory) {
        recognizerFactory.create(context.applicationContext)
    }
    var state by remember { mutableStateOf<VoiceRecognitionState>(VoiceRecognitionState.Idle) }
    var pendingPermissionStart by rememberSaveable { mutableStateOf(false) }

    fun cancelRecognition(notify: Boolean) {
        pendingPermissionStart = false
        recognizer.cancel()
        state = VoiceRecognitionState.Idle
        if (notify) currentOnResult(VoiceInputResult.Cancelled)
    }

    fun startRecognition() {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            pendingPermissionStart = true
            return
        }
        pendingPermissionStart = false
        state = VoiceRecognitionState.Listening()
        recognizer.start(
            VoiceRecognitionRequest(
                preferredLanguageTags = preferredSpeechLanguageTags(
                    configuration.locales[0],
                ),
                enableLanguageSwitch = true,
            ),
        ) { event ->
            when (event) {
                VoiceRecognitionEvent.Ready,
                VoiceRecognitionEvent.SpeechStarted -> {
                    val previous = state as? VoiceRecognitionState.Listening
                    state = previous ?: VoiceRecognitionState.Listening()
                }

                is VoiceRecognitionEvent.AudioLevelChanged -> {
                    val previous = state as? VoiceRecognitionState.Listening
                    state = VoiceRecognitionState.Listening(
                        partialText = previous?.partialText.orEmpty(),
                        rmsDb = event.rmsDb,
                    )
                }

                is VoiceRecognitionEvent.PartialResult -> {
                    val previous = state as? VoiceRecognitionState.Listening
                    state = VoiceRecognitionState.Listening(
                        partialText = event.text,
                        rmsDb = previous?.rmsDb,
                    )
                }

                VoiceRecognitionEvent.Processing -> {
                    val partial = when (val previous = state) {
                        is VoiceRecognitionState.Listening -> previous.partialText
                        is VoiceRecognitionState.Processing -> previous.partialText
                        VoiceRecognitionState.Idle -> ""
                    }
                    state = VoiceRecognitionState.Processing(partial)
                }

                is VoiceRecognitionEvent.FinalResult -> {
                    state = VoiceRecognitionState.Idle
                    currentOnResult(VoiceInputResult.Recognized(event.candidates))
                }

                VoiceRecognitionEvent.Empty -> {
                    state = VoiceRecognitionState.Idle
                    currentOnResult(VoiceInputResult.Empty)
                }

                VoiceRecognitionEvent.LanguageUnavailable -> {
                    state = VoiceRecognitionState.Idle
                    currentOnResult(VoiceInputResult.LanguageUnavailable)
                }

                VoiceRecognitionEvent.ModelDownloadRequired -> {
                    state = VoiceRecognitionState.Idle
                    currentOnResult(VoiceInputResult.ModelDownloadRequired)
                }

                VoiceRecognitionEvent.Unavailable -> {
                    state = VoiceRecognitionState.Idle
                    currentOnResult(VoiceInputResult.Unavailable)
                }

                VoiceRecognitionEvent.Failed -> {
                    state = VoiceRecognitionState.Idle
                    currentOnResult(VoiceInputResult.Failed)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingPermissionStart) {
            startRecognition()
        } else if (!granted) {
            pendingPermissionStart = false
            state = VoiceRecognitionState.Idle
            currentOnResult(VoiceInputResult.PermissionDenied)
        }
    }

    DisposableEffect(lifecycleOwner, recognizer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (
                        pendingPermissionStart &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startRecognition()
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    if (state !is VoiceRecognitionState.Idle) {
                        cancelRecognition(notify = false)
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recognizer.close()
        }
    }

    return TaskVoiceInput(
        state = state,
        launch = {
            if (state !is VoiceRecognitionState.Idle) {
                cancelRecognition(notify = true)
            } else if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                pendingPermissionStart = true
                startRecognition()
            } else {
                pendingPermissionStart = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        cancel = { cancelRecognition(notify = true) },
    )
}

internal fun preferredSpeechLanguageTags(locale: Locale): List<String> =
    listOf(locale.toLanguageTag())
