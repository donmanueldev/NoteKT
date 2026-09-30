package com.manuelduarte077.notyapp.features.notes.presentation.detail

sealed interface TaskEvent{
    data object TaskCreated: TaskEvent
    data object SaveFailed: TaskEvent
}
