package com.manuelduarte077.notyapp.features.notes.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

data class VoiceTaskDraft(
    val title: String,
    val description: String? = null,
    val category: Category? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
)

class VoiceTaskParser(
    private val lexicon: VoiceTaskLexicon,
) {
    private val leadingCommand = Regex(
        "^${lexicon.leadingCommands.toAlternation()}\\s+",
        RegexOption.IGNORE_CASE,
    )
    private val description = Regex(
        "(?:,?\\s*)(?:${lexicon.descriptionLabels.toAlternation()})\\s*:?\\s*(.+?)" +
            "(?=,?\\s+${metadataBoundary()}|$)",
        setOf(RegexOption.IGNORE_CASE),
    )
    private val time = Regex(
        "\\b(?:${lexicon.timePrefixes.toAlternation()})\\s+" +
            "(${lexicon.hourWords.values.flatten().toAlternation()}|\\d{1,2})" +
            "(?::(\\d{2}))?\\s*" +
            "(?:${lexicon.morningMarkers.plus(lexicon.afternoonMarkers).toAlternation()})?" +
            "(?:\\s+(?:${lexicon.fractionMinutes.values.flatten().toAlternation()}))?\\s*" +
            "(?:${lexicon.morningMarkers.plus(lexicon.afternoonMarkers).toAlternation()})?(?!\\w)",
        RegexOption.IGNORE_CASE,
    )

    fun parse(transcript: String, now: LocalDateTime = LocalDateTime.now()): VoiceTaskDraft {
        var remainder = transcript.trim().replace(WHITESPACE, " ")

        val descriptionMatch = description.find(remainder)
        val descriptionText = descriptionMatch?.groupValues?.get(1)
            ?.trim(' ', ',')
            ?.takeIf(String::isNotBlank)
            ?.sentenceCase()
        if (descriptionMatch != null) remainder = remainder.remove(descriptionMatch.range)

        val categoryMatch = findCategory(remainder)
        if (categoryMatch != null) remainder = remainder.remove(categoryMatch.second.range)

        val timeMatch = time.find(remainder)
        val dueTime = timeMatch?.let(::parseTime)
        if (timeMatch != null) remainder = remainder.remove(timeMatch.range)

        val dateMatch = findDate(remainder, now.toLocalDate())
        if (dateMatch != null) remainder = remainder.remove(dateMatch.second)

        val dueDate = dateMatch?.first ?: dueTime?.let {
            if (it.isAfter(now.toLocalTime())) now.toLocalDate() else now.toLocalDate().plusDays(1)
        }

        return VoiceTaskDraft(
            title = remainder.replace(leadingCommand, "")
                .replace(WHITESPACE, " ")
                .trim(' ', ',', '.', ':', ';', '-')
                .ifBlank { transcript.trim() }
                .sentenceCase(),
            description = descriptionText,
            category = categoryMatch?.first,
            dueDate = dueDate,
            dueTime = dueTime,
        )
    }

    private fun findCategory(text: String): Pair<Category, MatchResult>? =
        lexicon.categoryLabels.asSequence()
            .flatMap { (category, labels) -> labels.asSequence().map { category to it } }
            .mapNotNull { (category, label) ->
                Regex("(?:,?\\s*)\\b${Regex.escape(label)}\\b", RegexOption.IGNORE_CASE)
                    .find(text)
                    ?.let { category to it }
            }
            .firstOrNull()

    private fun findDate(text: String, today: LocalDate): Pair<LocalDate, IntRange>? {
        lexicon.weekdayLabels.forEach { (day, labels) ->
            labels.sortedByDescending(String::length).forEach { label ->
                Regex("\\b${Regex.escape(label)}\\b", RegexOption.IGNORE_CASE).find(text)?.let {
                    return today.with(TemporalAdjusters.next(day)) to it.range
                }
            }
        }
        lexicon.relativeDateLabels.forEach { (days, labels) ->
            labels.sortedByDescending(String::length).forEach { label ->
                Regex("\\b${Regex.escape(label)}\\b", RegexOption.IGNORE_CASE).find(text)?.let {
                    return today.plusDays(days) to it.range
                }
            }
        }
        return null
    }

    private fun parseTime(match: MatchResult): LocalTime? {
        val hour = match.groupValues[1].toNumber() ?: return null
        val minute = match.groupValues[2].toIntOrNull()
            ?: lexicon.fractionMinutes.entries.firstNotNullOfOrNull { (value, labels) ->
                value.takeIf { amount -> labels.any { match.value.contains(it, ignoreCase = true) } }
            }
            ?: 0
        val period = match.value.lowercase()
        val afternoon = lexicon.afternoonMarkers.any { period.contains(it.lowercase()) }
        val morning = lexicon.morningMarkers.any { period.contains(it.lowercase()) }
        val resolvedHour = when {
            afternoon && hour in 1..11 -> hour + 12
            morning && hour == 12 -> 0
            else -> hour
        }
        return resolvedHour.takeIf { it in 0..23 }?.let { LocalTime.of(it, minute) }
    }

    private fun metadataBoundary(): String = buildString {
        val labels = buildList {
            addAll(lexicon.relativeDateLabels.values.flatten())
            addAll(lexicon.weekdayLabels.values.flatten())
            addAll(lexicon.timePrefixes)
            addAll(lexicon.categoryLabels.values.flatten())
        }
        append("(?:${labels.toAlternation()})\\b")
    }

    private fun String.remove(range: IntRange): String =
        removeRange(range.first, range.last + 1).replace(WHITESPACE, " ").trim()

    private fun String.toNumber(): Int? = toIntOrNull() ?: lexicon.hourWords.entries
        .firstOrNull { (_, words) -> words.any { it.equals(this, ignoreCase = true) } }
        ?.key

    private fun String.sentenceCase(): String = replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase() else character.toString()
    }

    private fun List<String>.toAlternation(): String =
        sortedByDescending(String::length).joinToString("|") { Regex.escape(it) }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
