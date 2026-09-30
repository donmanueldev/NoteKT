package com.manuelduarte077.notyapp.features.notes.presentation.detail

import android.content.res.Resources
import com.manuelduarte077.notyapp.R
import com.manuelduarte077.notyapp.features.notes.domain.Category
import com.manuelduarte077.notyapp.features.notes.domain.VoiceTaskLexicon
import java.time.DayOfWeek

internal fun loadVoiceTaskLexicon(resources: Resources): VoiceTaskLexicon = VoiceTaskLexicon(
    leadingCommands = resources.getStringArray(R.array.voice_leading_commands).toList(),
    categoryLabels = mapOf(
        Category.WORK to resources.getStringArray(R.array.voice_category_work).toList(),
        Category.PERSONAL to resources.getStringArray(R.array.voice_category_personal).toList(),
        Category.SHOPPING to resources.getStringArray(R.array.voice_category_shopping).toList(),
        Category.OTHER to resources.getStringArray(R.array.voice_category_other).toList(),
    ),
    descriptionLabels = resources.getStringArray(R.array.voice_description_labels).toList(),
    relativeDateLabels = mapOf(
        0L to resources.getStringArray(R.array.voice_date_today).toList(),
        1L to resources.getStringArray(R.array.voice_date_tomorrow).toList(),
        2L to resources.getStringArray(R.array.voice_date_after_tomorrow).toList(),
    ),
    weekdayLabels = mapOf(
        DayOfWeek.MONDAY to resources.getStringArray(R.array.voice_weekday_monday).toList(),
        DayOfWeek.TUESDAY to resources.getStringArray(R.array.voice_weekday_tuesday).toList(),
        DayOfWeek.WEDNESDAY to resources.getStringArray(R.array.voice_weekday_wednesday).toList(),
        DayOfWeek.THURSDAY to resources.getStringArray(R.array.voice_weekday_thursday).toList(),
        DayOfWeek.FRIDAY to resources.getStringArray(R.array.voice_weekday_friday).toList(),
        DayOfWeek.SATURDAY to resources.getStringArray(R.array.voice_weekday_saturday).toList(),
        DayOfWeek.SUNDAY to resources.getStringArray(R.array.voice_weekday_sunday).toList(),
    ),
    timePrefixes = resources.getStringArray(R.array.voice_time_prefixes).toList(),
    morningMarkers = resources.getStringArray(R.array.voice_morning_markers).toList(),
    afternoonMarkers = resources.getStringArray(R.array.voice_afternoon_markers).toList(),
    fractionMinutes = mapOf(
        30 to resources.getStringArray(R.array.voice_fraction_half).toList(),
        15 to resources.getStringArray(R.array.voice_fraction_quarter).toList(),
    ),
    hourWords = mapOf(
        0 to resources.getStringArray(R.array.voice_hour_zero).toList(),
        1 to resources.getStringArray(R.array.voice_hour_one).toList(),
        2 to resources.getStringArray(R.array.voice_hour_two).toList(),
        3 to resources.getStringArray(R.array.voice_hour_three).toList(),
        4 to resources.getStringArray(R.array.voice_hour_four).toList(),
        5 to resources.getStringArray(R.array.voice_hour_five).toList(),
        6 to resources.getStringArray(R.array.voice_hour_six).toList(),
        7 to resources.getStringArray(R.array.voice_hour_seven).toList(),
        8 to resources.getStringArray(R.array.voice_hour_eight).toList(),
        9 to resources.getStringArray(R.array.voice_hour_nine).toList(),
        10 to resources.getStringArray(R.array.voice_hour_ten).toList(),
        11 to resources.getStringArray(R.array.voice_hour_eleven).toList(),
        12 to resources.getStringArray(R.array.voice_hour_twelve).toList(),
        13 to resources.getStringArray(R.array.voice_hour_thirteen).toList(),
        14 to resources.getStringArray(R.array.voice_hour_fourteen).toList(),
        15 to resources.getStringArray(R.array.voice_hour_fifteen).toList(),
        16 to resources.getStringArray(R.array.voice_hour_sixteen).toList(),
        17 to resources.getStringArray(R.array.voice_hour_seventeen).toList(),
        18 to resources.getStringArray(R.array.voice_hour_eighteen).toList(),
        19 to resources.getStringArray(R.array.voice_hour_nineteen).toList(),
        20 to resources.getStringArray(R.array.voice_hour_twenty).toList(),
        21 to resources.getStringArray(R.array.voice_hour_twenty_one).toList(),
        22 to resources.getStringArray(R.array.voice_hour_twenty_two).toList(),
        23 to resources.getStringArray(R.array.voice_hour_twenty_three).toList(),
    ),
)
