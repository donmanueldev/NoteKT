package com.manuelduarte077.notyapp.features.notes.presentation.home

import com.manuelduarte077.notyapp.features.notes.domain.Task

sealed interface HomeScreenAction {
    data class OnToggleTask(val task: Task) : HomeScreenAction
    data class OnDeleteTask(val task: Task) : HomeScreenAction
    data object OnDeleteAllTasks : HomeScreenAction
    data object OnAddTask : HomeScreenAction
    data object OnAddTaskByVoice : HomeScreenAction
    data class OnClickTask(val taskId: String) : HomeScreenAction
}
