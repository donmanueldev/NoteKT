package com.manuelduarte077.notyapp.features.notes.data

import androidx.room.Database
import androidx.room.AutoMigration
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)

abstract class TodoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
