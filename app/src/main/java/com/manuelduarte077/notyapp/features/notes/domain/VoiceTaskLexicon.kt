package com.manuelduarte077.notyapp.features.notes.domain

import java.time.DayOfWeek

data class VoiceTaskLexicon(
    val leadingCommands: List<String>,
    val categoryLabels: Map<Category, List<String>>,
    val descriptionLabels: List<String>,
    val relativeDateLabels: Map<Long, List<String>>,
    val weekdayLabels: Map<DayOfWeek, List<String>>,
    val timePrefixes: List<String>,
    val morningMarkers: List<String>,
    val afternoonMarkers: List<String>,
    val fractionMinutes: Map<Int, List<String>>,
    val hourWords: Map<Int, List<String>>,
) {
    init {
        require(leadingCommands.none(String::isBlank))
        require(descriptionLabels.none(String::isBlank))
        require(timePrefixes.none(String::isBlank))
    }
}
