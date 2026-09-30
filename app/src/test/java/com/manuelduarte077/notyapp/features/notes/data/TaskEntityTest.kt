package com.manuelduarte077.notyapp.features.notes.data

import com.manuelduarte077.notyapp.features.notes.domain.Category
import com.manuelduarte077.notyapp.features.notes.domain.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskEntityTest {
    @Test
    fun `round trip preserves creation timestamp and deadline independently`() {
        val task = Task(
            id = "task-1",
            title = "Preparar informe",
            description = "Ventas del mes",
            category = Category.WORK,
            date = LocalDateTime.of(2025, 2, 3, 10, 15),
            dueDate = LocalDate.of(2026, 9, 30),
            dueTime = LocalTime.of(16, 30),
        )

        assertEquals(task, TaskEntity.fromTask(task).toTask())
    }
}
