package com.manuelduarte077.notyapp.features.notes.domain

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val isCompleted: Boolean = false,
    val category: Category? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
) {
    init {
        require(dueTime == null || dueDate != null) {
            "A task due time requires a due date"
        }
    }
}
