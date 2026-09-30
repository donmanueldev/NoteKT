package com.manuelduarte077.notyapp.features.notes.presentation.detail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.manuelduarte077.notyapp.features.notes.presentation.detail.providers.TaskScreenStatePreviewProvider
import com.manuelduarte077.notyapp.R
import com.manuelduarte077.notyapp.features.notes.domain.Category
import com.manuelduarte077.notyapp.features.notes.domain.VoiceTaskParser
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.AndroidVoiceRecognizerFactory
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognitionCandidate
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognitionState
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognizerFactory
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceInputResult
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.rememberTaskVoiceInput
import com.manuelduarte077.notyapp.ui.theme.NoteTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TaskScreenRoot(
    navigateBack: () -> Boolean,
    viewModel: TaskViewModel,
    voiceRecognizerFactory: VoiceRecognizerFactory = AndroidVoiceRecognizerFactory,
) {
    val state = viewModel.state
    val event = viewModel.event

    val context = LocalContext.current
    val voiceTaskParser = remember(context.resources) {
        VoiceTaskParser(loadVoiceTaskLexicon(context.resources))
    }
    var voiceCandidates by remember { mutableStateOf<List<VoiceRecognitionCandidate>>(emptyList()) }
    val voiceInput = rememberTaskVoiceInput(
        recognizerFactory = voiceRecognizerFactory,
    ) { result ->
        when (result) {
            is VoiceInputResult.Recognized -> {
                voiceCandidates = result.candidates
                result.candidates.firstOrNull()?.let { candidate ->
                    viewModel.onAction(
                        ActionTask.ApplyDictatedTask(voiceTaskParser.parse(candidate.text)),
                    )
                }
            }
            VoiceInputResult.Cancelled -> voiceCandidates = emptyList()
            else -> {
                voiceCandidates = emptyList()
                val message = when (result) {
                    VoiceInputResult.Empty -> R.string.voice_input_empty
                    VoiceInputResult.Unavailable -> R.string.voice_input_unavailable
                    VoiceInputResult.PermissionDenied -> R.string.voice_input_permission_denied
                    VoiceInputResult.LanguageUnavailable -> R.string.voice_input_language_unavailable
                    VoiceInputResult.ModelDownloadRequired -> R.string.voice_input_model_downloading
                    else -> R.string.voice_input_failed
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(viewModel) {
        if (viewModel.consumeInitialVoiceInput()) {
            voiceCandidates = emptyList()
            voiceInput.launch()
        }
    }

    LaunchedEffect(true) {
        event.collect { event ->
            when (event) {
                is TaskEvent.TaskCreated -> {
                    Toast.makeText(
                        context,
                        R.string.task_created,
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateBack()
                }
                TaskEvent.SaveFailed -> Toast.makeText(
                    context,
                    R.string.error_creating_task,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    TaskScreen(
        state = state,
        voiceRecognitionState = voiceInput.state,
        voiceCandidates = voiceCandidates,
        onDictateTitle = {
            voiceCandidates = emptyList()
            voiceInput.launch()
        },
        onSelectVoiceCandidate = { candidate ->
            voiceCandidates = emptyList()
            viewModel.onAction(
                ActionTask.ApplyDictatedTask(voiceTaskParser.parse(candidate.text)),
            )
        },
        onAction = { action ->
            when (action) {
                is ActionTask.Back -> {
                    navigateBack()
                }

                else -> {
                    voiceCandidates = emptyList()
                    viewModel.onAction(action)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskScreen(
    state: TaskScreenState,
    onAction: (ActionTask) -> Unit,
    voiceRecognitionState: VoiceRecognitionState = VoiceRecognitionState.Idle,
    voiceCandidates: List<VoiceRecognitionCandidate> = emptyList(),
    onDictateTitle: () -> Unit = {},
    onSelectVoiceCandidate: (VoiceRecognitionCandidate) -> Unit = {},
) {

    var isDescriptionFocus by remember {
        mutableStateOf(false)
    }
    var isExpanded by remember {
        mutableStateOf(false)
    }
    var showVoiceAlternatives by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isVoiceInputInProgress = voiceRecognitionState !is VoiceRecognitionState.Idle

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        style = MaterialTheme.typography.headlineSmall,
                        text = stringResource(R.string.task)
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable {
                            onAction(
                                ActionTask.Back
                            )
                        }
                    )
                },
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(
                8.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding()

        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.done),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(8.dp)
                )
                Checkbox(
                    checked = state.isTaskDone,
                    onCheckedChange = {
                        onAction(
                            ActionTask.ChangeTaskDone(
                                isTaskDone = it
                            )
                        )
                    },
                )
                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        isExpanded = true
                    }
                ) {

                    Text(
                        text = state.category?.toString() ?: stringResource(R.string.category),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    )
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        DropdownMenu(
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            Column {
                                Category.entries.forEach { category ->
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .padding(
                                                8.dp
                                            )
                                            .clickable {
                                                isExpanded = false
                                                onAction(
                                                    ActionTask.ChangeTaskCategory(
                                                        category = category
                                                    )
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }


            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    state = state.taskName,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight(),
                    decorator = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (state.taskName.text.toString().isEmpty()) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(R.string.task_name),
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.5f
                                    ),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (state.isNewTask) {
                    IconButton(
                        enabled = !state.isSaving,
                        onClick = onDictateTitle,
                    ) {
                        Icon(
                            imageVector = if (isVoiceInputInProgress) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = stringResource(
                                if (isVoiceInputInProgress) {
                                    R.string.stop_voice_input
                                } else {
                                    R.string.dictate_task_title
                                },
                            ),
                        )
                    }
                }
            }
            when (voiceRecognitionState) {
                is VoiceRecognitionState.Listening -> {
                    val audioLevel = voiceRecognitionState.rmsDb
                        ?.let { ((it + 2f) / 12f).coerceIn(0f, 1f) }
                    if (audioLevel == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { audioLevel },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = voiceRecognitionState.partialText.ifBlank {
                            stringResource(R.string.voice_input_listening)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is VoiceRecognitionState.Processing -> Text(
                    text = voiceRecognitionState.partialText.ifBlank {
                        stringResource(R.string.voice_input_processing)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                VoiceRecognitionState.Idle -> Unit
            }
            if (voiceCandidates.size > 1) {
                TextButton(onClick = { showVoiceAlternatives = true }) {
                    Text(stringResource(R.string.voice_input_alternatives, voiceCandidates.size))
                }
            }
            BasicTextField(
                state = state.taskDescription,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                lineLimits = if (isDescriptionFocus)
                    TextFieldLineLimits.MultiLine(
                        minHeightInLines = 1,
                        maxHeightInLines = 5
                    )
                else
                    TextFieldLineLimits.Default,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onFocusChanged {
                        isDescriptionFocus = it.isFocused
                    },
                decorator = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (state.taskDescription.text.toString()
                                .isEmpty() && !isDescriptionFocus
                        ) {
                            Text(
                                text = stringResource(R.string.task_description),
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.5f
                                )
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        val initialDate = state.dueDate ?: LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                onAction(
                                    ActionTask.ChangeTaskDueDate(
                                        LocalDate.of(year, month + 1, day),
                                    ),
                                )
                            },
                            initialDate.year,
                            initialDate.monthValue - 1,
                            initialDate.dayOfMonth,
                        ).show()
                    },
                ) {
                    Text(
                        state.dueDate?.format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
                        ) ?: stringResource(R.string.add_due_date),
                    )
                }
                TextButton(
                    onClick = {
                        val initialTime = state.dueTime ?: LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                if (state.dueDate == null) {
                                    onAction(ActionTask.ChangeTaskDueDate(LocalDate.now()))
                                }
                                onAction(ActionTask.ChangeTaskDueTime(LocalTime.of(hour, minute)))
                            },
                            initialTime.hour,
                            initialTime.minute,
                            false,
                        ).show()
                    },
                ) {
                    Text(
                        state.dueTime?.format(
                            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
                        ) ?: stringResource(R.string.add_due_time),
                    )
                }
                if (state.dueDate != null || state.dueTime != null) {
                    TextButton(
                        onClick = {
                            onAction(ActionTask.ChangeTaskDueDate(null))
                            onAction(ActionTask.ChangeTaskDueTime(null))
                        },
                    ) {
                        Text(stringResource(R.string.clear_due_date))
                    }
                }
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )


            Button(
                enabled = state.canSaveTask,
                onClick = {
                    onAction(
                        ActionTask.SaveTask
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(46.dp)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.canSaveTask)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    if (showVoiceAlternatives) {
        AlertDialog(
            onDismissRequest = { showVoiceAlternatives = false },
            title = { Text(stringResource(R.string.voice_input_choose_result)) },
            text = {
                Column {
                    voiceCandidates.forEach { candidate ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showVoiceAlternatives = false
                                onSelectVoiceCandidate(candidate)
                            },
                        ) {
                            val confidence = candidate.confidence?.let {
                                " (${(it * 100).toInt()}%)"
                            }.orEmpty()
                            Text(candidate.text + confidence)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceAlternatives = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
@Preview
fun TaskScreenLightPreview(
    @PreviewParameter(TaskScreenStatePreviewProvider::class) state: TaskScreenState
) {
    NoteTheme {
        TaskScreen(
            state = state,
            onAction = {}
        )
    }
}

@Composable
@Preview(
    uiMode = UI_MODE_NIGHT_YES
)
fun TaskScreenDarkPreview(
    @PreviewParameter(TaskScreenStatePreviewProvider::class) state: TaskScreenState
) {
    NoteTheme {
        TaskScreen(
            state = state,
            onAction = {}
        )
    }
}
