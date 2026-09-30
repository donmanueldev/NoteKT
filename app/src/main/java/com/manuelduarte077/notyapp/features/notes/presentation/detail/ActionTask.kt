package com.manuelduarte077.notyapp.features.notes.presentation.detail

import com.manuelduarte077.notyapp.features.notes.domain.Category
import com.manuelduarte077.notyapp.features.notes.domain.VoiceTaskDraft
import java.time.LocalDate
import java.time.LocalTime

sealed interface ActionTask {
    data object SaveTask : ActionTask
    data object Back : ActionTask
    data class ApplyDictatedTask(val draft: VoiceTaskDraft) : ActionTask
    data class ChangeTaskCategory(val category: Category?) : ActionTask
    data class ChangeTaskDone(val isTaskDone: Boolean) : ActionTask
    data class ChangeTaskDueDate(val dueDate: LocalDate?) : ActionTask
    data class ChangeTaskDueTime(val dueTime: LocalTime?) : ActionTask
}
