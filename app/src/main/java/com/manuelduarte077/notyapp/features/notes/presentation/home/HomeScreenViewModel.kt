package com.manuelduarte077.notyapp.features.notes.presentation.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelduarte077.notyapp.analytics.AnalyticsTracker
import com.manuelduarte077.notyapp.features.notes.domain.TaskLocalDataSource
import com.manuelduarte077.notyapp.features.notes.presentation.home.HomeScreenAction.OnDeleteAllTasks
import com.manuelduarte077.notyapp.features.notes.presentation.home.HomeScreenAction.OnDeleteTask
import com.manuelduarte077.notyapp.features.notes.presentation.home.HomeScreenAction.OnToggleTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val taskLocalDataSource: TaskLocalDataSource,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    var state by mutableStateOf(HomeDataState())
        private set

    private val eventChannel =
        Channel<HomeScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        analyticsTracker.logScreenView("home")

        state = state.copy(
            date = LocalDate.now().let {
                DateTimeFormatter.ofPattern("EEEE, MMMM dd yyyy").format(it)
            }
        )

        taskLocalDataSource.tasksFlow.onEach { tasks ->

            val completedTasks = tasks
                .filter { task -> task.isCompleted }
                .sortedByDescending { task ->
                    task.date
                }
            val pendingTasks = tasks
                .filter { task ->
                    !task.isCompleted
                }.sortedByDescending { task ->
                    task.date
                }

            state = state.copy(
                date = state.date,
                summary = pendingTasks.size.toString(),
                completedTask = completedTasks,
                pendingTask = pendingTasks
            )
        }.launchIn(viewModelScope)

    }


    fun onAction(action: HomeScreenAction) {
        viewModelScope.launch {
            when (action) {

                is OnDeleteTask -> {
                    taskLocalDataSource.removeTask(action.task)
                    analyticsTracker.logEvent("task_deleted")
                    eventChannel.send(HomeScreenEvent.DeletedTask)
                }

                is OnToggleTask -> {
                    val updatedTask = action.task.copy(isCompleted = !action.task.isCompleted)
                    taskLocalDataSource.updateTask(updatedTask)
                    analyticsTracker.logEvent(
                        name = "task_status_toggled",
                        parameters = mapOf("is_completed" to updatedTask.isCompleted.toString()),
                    )
                    eventChannel.send(HomeScreenEvent.UpdatedTask)
                }

                OnDeleteAllTasks -> {
                    taskLocalDataSource.removeAllTasks()
                    analyticsTracker.logEvent("all_tasks_deleted")
                    eventChannel.send(HomeScreenEvent.AllTaskDeleted)
                }

                else -> Unit
            }
        }
    }
}
