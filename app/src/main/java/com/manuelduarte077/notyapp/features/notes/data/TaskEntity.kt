package com.manuelduarte077.notyapp.features.notes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manuelduarte077.notyapp.features.notes.domain.Category
import com.manuelduarte077.notyapp.features.notes.domain.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Entity(tableName = "notes")
data class TaskEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val title: String,
    val description: String?,
    val category: Int?,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    val date: Long,
    @ColumnInfo(name = "due_date")
    val dueDate: Long?,
    @ColumnInfo(name = "due_time")
    val dueTime: Int?,
) {
    companion object {
        fun fromTask(task: Task): TaskEntity {
            return TaskEntity(
                id = task.id,
                title = task.title,
                description = task.description,
                isCompleted = task.isCompleted,
                category = task.category?.storageValue,
                date = task.date
                    .atZone(
                        ZoneId.systemDefault()
                    ).toInstant()
                    .toEpochMilli(),
                dueDate = task.dueDate?.toEpochDay(),
                dueTime = task.dueTime?.let { it.hour * 60 + it.minute },
            )
        }
    }

    fun toTask(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted,
            category = category?.let { Category.fromStorageValue(it) },
            date = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(date),
                ZoneId.systemDefault()
            ),
            dueDate = dueDate?.let(LocalDate::ofEpochDay),
            dueTime = dueTime?.let { LocalTime.of(it / 60, it % 60) },
        )
    }
}
