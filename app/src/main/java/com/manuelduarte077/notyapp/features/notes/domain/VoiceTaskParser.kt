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

class VoiceTaskParser {
    fun parse(
        transcript: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): VoiceTaskDraft {
        var remainder = transcript.trim().replace(WHITESPACE, " ")

        val descriptionMatch = DESCRIPTION.find(remainder)
        val description = descriptionMatch
            ?.groupValues
            ?.get(1)
            ?.trim(' ', ',')
            ?.takeIf(String::isNotBlank)
            ?.sentenceCase()
        if (descriptionMatch != null) remainder = remainder.remove(descriptionMatch.range)

        val categoryMatch = CATEGORY.find(remainder)
        val category = categoryMatch?.groupValues?.get(1)?.toCategory()
        if (categoryMatch != null) remainder = remainder.remove(categoryMatch.range)

        val (dueTime, timeRange) = parseTime(remainder)
        if (timeRange != null) remainder = remainder.remove(timeRange)

        // Remove the full time phrase first so "de la mañana" is not read as
        // the relative date "mañana".
        val (dueDate, dateRange) = parseDate(remainder, now.toLocalDate())
        if (dateRange != null) remainder = remainder.remove(dateRange)

        val resolvedDate = dueDate ?: dueTime?.let {
            if (it.isAfter(now.toLocalTime())) now.toLocalDate() else now.toLocalDate().plusDays(1)
        }
        val cleanedTitle = remainder
            .replace(LEADING_COMMAND, "")
            .replace(WHITESPACE, " ")
            .trim(' ', ',', '.', ':', ';', '-')
            .ifBlank { transcript.trim() }
            .sentenceCase()

        return VoiceTaskDraft(
            title = cleanedTitle,
            description = description,
            category = category,
            dueDate = resolvedDate,
            dueTime = dueTime,
        )
    }

    private fun parseDate(text: String, today: LocalDate): Pair<LocalDate?, IntRange?> {
        WEEKDAY.find(text)?.let { match ->
            val dayOfWeek = when (match.groupValues[1].lowercase()) {
                "lunes" -> DayOfWeek.MONDAY
                "martes" -> DayOfWeek.TUESDAY
                "miércoles", "miercoles" -> DayOfWeek.WEDNESDAY
                "jueves" -> DayOfWeek.THURSDAY
                "viernes" -> DayOfWeek.FRIDAY
                "sábado", "sabado" -> DayOfWeek.SATURDAY
                else -> DayOfWeek.SUNDAY
            }
            return today.with(TemporalAdjusters.next(dayOfWeek)) to match.range
        }

        RELATIVE_DATE.find(text)?.let { match ->
            val days = when (match.value.lowercase()) {
                "hoy" -> 0L
                "mañana", "manana" -> 1L
                else -> 2L
            }
            return today.plusDays(days) to match.range
        }

        return null to null
    }

    private fun parseTime(text: String): Pair<LocalTime?, IntRange?> {
        val match = TIME.find(text) ?: return null to null
        var hour = match.groupValues[1].toNumber() ?: return null to null
        var minute = match.groupValues[2].toIntOrNull() ?: 0
        val period = listOf(match.groupValues[3], match.groupValues[5])
            .filter(String::isNotBlank)
            .joinToString(" ")
            .lowercase()
            .replace(Regex("[.\\s]"), "")
        val fraction = match.groupValues[4].lowercase()

        if (fraction.contains("media")) minute = 30
        if (fraction.contains("cuarto")) minute = 15
        when {
            period == "pm" || period.contains("tarde") || period.contains("noche") -> {
                if (hour in 1..11) hour += 12
            }

            period == "am" || period.contains("mañana") || period.contains("manana") -> {
                if (hour == 12) hour = 0
            }
        }

        return if (hour in 0..23 && minute in 0..59) {
            LocalTime.of(hour, minute) to match.range
        } else {
            null to null
        }
    }

    private fun String.remove(range: IntRange): String =
        removeRange(range.first, range.last + 1).replace(WHITESPACE, " ").trim()

    private fun String.toCategory(): Category? = when (lowercase()) {
        "trabajo", "laboral" -> Category.WORK
        "personal" -> Category.PERSONAL
        "compra", "compras" -> Category.SHOPPING
        "otro", "otros" -> Category.OTHER
        else -> null
    }

    private fun String.toNumber(): Int? = toIntOrNull() ?: when (lowercase()) {
        "cero" -> 0
        "una", "uno" -> 1
        "dos" -> 2
        "tres" -> 3
        "cuatro" -> 4
        "cinco" -> 5
        "seis" -> 6
        "siete" -> 7
        "ocho" -> 8
        "nueve" -> 9
        "diez" -> 10
        "once" -> 11
        "doce" -> 12
        "trece" -> 13
        "catorce" -> 14
        "quince" -> 15
        "dieciséis", "dieciseis" -> 16
        "diecisiete" -> 17
        "dieciocho" -> 18
        "diecinueve" -> 19
        "veinte" -> 20
        "veintiuno" -> 21
        "veintidós", "veintidos" -> 22
        "veintitrés", "veintitres" -> 23
        else -> null
    }

    private fun String.sentenceCase(): String = replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase() else character.toString()
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val LEADING_COMMAND = Regex(
            "^(?:(?:recu[eé]rdame|recordame)(?:\\s+que)?|(?:crea|crear|agrega|agregar)" +
                "(?:\\s+una)?\\s+tarea(?:\\s+para)?|anota(?:r)?(?:\\s+que)?)\\s+",
            RegexOption.IGNORE_CASE,
        )
        val CATEGORY = Regex(
            "(?:,?\\s*)\\b(?:categor[ií]a\\s+|para\\s+)(trabajo|laboral|personal|compras?|otros?)\\b",
            RegexOption.IGNORE_CASE,
        )
        val DESCRIPTION = Regex(
            "(?:,?\\s*)(?:con\\s+)?(?:descripci[oó]n|detalles?)\\s*:?\\s*(.+?)" +
                "(?=,?\\s+(?:categor[ií]a\\s+|para\\s+(?:trabajo|laboral|personal|compras?|otros?)\\b|" +
                "hoy\\b|ma[ñn]ana\\b|pasado\\s+ma[ñn]ana\\b|(?:el\\s+)?" +
                "(?:lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo)\\b|a\\s+la(?:s)?\\b)|$)",
            setOf(RegexOption.IGNORE_CASE),
        )
        val RELATIVE_DATE = Regex(
            "\\b(?:pasado\\s+ma[ñn]ana|ma[ñn]ana|hoy)\\b",
            RegexOption.IGNORE_CASE,
        )
        val WEEKDAY = Regex(
            "\\b(?:el\\s+)?(?:pr[oó]ximo\\s+)?" +
                "(lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo)\\b",
            RegexOption.IGNORE_CASE,
        )
        val TIME = Regex(
            "\\b(?:a\\s+la(?:s)?)\\s+" +
                "(\\d{1,2}|cero|una|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce|" +
                "trece|catorce|quince|diecis[eé]is|diecisiete|dieciocho|diecinueve|veinte|veintiuno|" +
                "veintid[oó]s|veintitr[eé]s)" +
                "(?::(\\d{2}))?\\s*" +
                "(a\\.?\\s*m\\.?|p\\.?\\s*m\\.?|de\\s+la\\s+ma[ñn]ana|de\\s+la\\s+tarde|" +
                "de\\s+la\\s+noche)?\\s*" +
                "(?:y\\s+(media|cuarto))?\\s*" +
                "(a\\.?\\s*m\\.?|p\\.?\\s*m\\.?|de\\s+la\\s+ma[ñn]ana|de\\s+la\\s+tarde|" +
                "de\\s+la\\s+noche)?\\b",
            RegexOption.IGNORE_CASE,
        )
    }
}
