package com.manuelduarte077.notyapp.features.notes.presentation.detail

import android.os.Bundle
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.manuelduarte077.notyapp.analytics.AnalyticsTracker
import com.manuelduarte077.notyapp.features.notes.domain.Category
import com.manuelduarte077.notyapp.features.notes.domain.Task
import com.manuelduarte077.notyapp.features.notes.domain.TaskLocalDataSource
import com.manuelduarte077.notyapp.navigation.TaskScreenDes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val localDataSource: TaskLocalDataSource,
    private val analyticsTracker: AnalyticsTracker,
): ViewModel() {


    private val taskData = savedStateHandle.toRoute<TaskScreenDes>()
    private val draft = if (taskData.taskId == null) savedStateHandle.get<Bundle>(DRAFT_KEY) else null

    var state by mutableStateOf(
        TaskScreenState(
            taskName = TextFieldState(draft?.getString("title").orEmpty()),
            taskDescription = TextFieldState(draft?.getString("description").orEmpty()),
            isTaskDone = draft?.getBoolean("completed") ?: false,
            category = Category.entries.firstOrNull { it.name == draft?.getString("category") },
            dueDate = draft
                ?.takeIf { it.containsKey("dueDate") }
                ?.getLong("dueDate")
                ?.let(LocalDate::ofEpochDay),
            dueTime = draft
                ?.takeIf { it.containsKey("dueTime") }
                ?.getInt("dueTime")
                ?.let { LocalTime.of(it / 60, it % 60) },
            isNewTask = taskData.taskId == null,
        ),
    )
        private set

    private var eventChannel = Channel<TaskEvent>()

    val event = eventChannel.receiveAsFlow()

    private var editedTask: Task? = null

    init {
        if (state.isNewTask) {
            savedStateHandle.setSavedStateProvider(DRAFT_KEY) {
                Bundle().apply {
                    putString("title", state.taskName.text.toString())
                    putString("description", state.taskDescription.text.toString())
                    putBoolean("completed", state.isTaskDone)
                    putString("category", state.category?.name)
                    state.dueDate?.let { putLong("dueDate", it.toEpochDay()) }
                    state.dueTime?.let { putInt("dueTime", it.hour * 60 + it.minute) }
                }
            }
        }
        analyticsTracker.logScreenView(
            if (taskData.taskId == null) "task_create" else "task_detail"
        )

        taskData.taskId?.let {
            viewModelScope.launch {
                localDataSource.getTaskById(taskData.taskId)?.let { task ->
                    editedTask= task
                    state = state.copy(
                        taskName = TextFieldState(task.title),
                        taskDescription = TextFieldState(task.description?:""),
                        isTaskDone = task.isCompleted,
                        category = task.category,
                        dueDate = task.dueDate,
                        dueTime = task.dueTime,
                    )
                }
            }
        }

    }

    fun consumeInitialVoiceInput(): Boolean {
        if (!state.isNewTask || !taskData.startVoiceInput ||
            savedStateHandle.get<Boolean>(VOICE_INPUT_STARTED_KEY) == true
        ) {
            return false
        }
        savedStateHandle[VOICE_INPUT_STARTED_KEY] = true
        return true
    }

    fun onAction(action: ActionTask) {
        when (action) {
            is ActionTask.ChangeTaskCategory -> state = state.copy(category = action.category)
            is ActionTask.ChangeTaskDone -> state = state.copy(isTaskDone = action.isTaskDone)
            is ActionTask.ChangeTaskDueDate -> state = state.copy(
                dueDate = action.dueDate,
                dueTime = state.dueTime.takeIf { action.dueDate != null },
            )
            is ActionTask.ChangeTaskDueTime -> state = state.copy(dueTime = action.dueTime)
            is ActionTask.ApplyDictatedTitle -> {
                val title = action.transcript.trim()
                if (state.isNewTask && !state.isSaving && title.isNotBlank()) {
                    state.taskName.setTextAndPlaceCursorAtEnd(title)
                }
            }
            ActionTask.SaveTask -> saveTask()
            ActionTask.Back -> Unit
        }
    }

    private fun saveTask() {
        if (!state.canSaveTask) return
        val task = editedTask?.copy(
            title = state.taskName.text.toString(),
            description = state.taskDescription.text.toString(),
            isCompleted = state.isTaskDone,
            category = state.category,
            dueDate = state.dueDate,
            dueTime = state.dueTime,
        ) ?: Task(
            id = UUID.randomUUID().toString(),
            title = state.taskName.text.toString(),
            description = state.taskDescription.text.toString(),
            isCompleted = state.isTaskDone,
            category = state.category,
            dueDate = state.dueDate,
            dueTime = state.dueTime,
        )
        state = state.copy(isSaving = true)
        viewModelScope.launch {
            try {
                if (editedTask == null) {
                    localDataSource.addTask(task)
                } else {
                    localDataSource.updateTask(task)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                state = state.copy(isSaving = false)
                eventChannel.send(TaskEvent.SaveFailed)
                return@launch
            }
            val category = task.category?.name?.let { mapOf("category" to it) }.orEmpty()
            analyticsTracker.logEvent(if (editedTask == null) "task_created" else "task_updated", category)
            eventChannel.send(TaskEvent.TaskCreated)
        }
    }

    private companion object {
        const val DRAFT_KEY = "task_draft"
        const val VOICE_INPUT_STARTED_KEY = "voice_input_started"
    }
}
