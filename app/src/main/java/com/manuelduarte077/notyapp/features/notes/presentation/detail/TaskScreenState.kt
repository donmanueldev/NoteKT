package com.manuelduarte077.notyapp.features.notes.presentation.detail

import androidx.compose.foundation.text.input.TextFieldState
import com.manuelduarte077.notyapp.features.notes.domain.Category
import java.time.LocalDate
import java.time.LocalTime

data class TaskScreenState(
    val taskName:TextFieldState = TextFieldState(),
    val taskDescription: TextFieldState = TextFieldState(),
    val isTaskDone: Boolean = false,
    val category: Category? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val isNewTask: Boolean = true,
    val isSaving: Boolean = false
) {
    val canSaveTask: Boolean
        get() = taskName.text.isNotBlank() && !isSaving
}
