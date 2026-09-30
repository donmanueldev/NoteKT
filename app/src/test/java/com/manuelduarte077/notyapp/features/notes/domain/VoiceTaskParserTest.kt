package com.manuelduarte077.notyapp.features.notes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.DayOfWeek

class VoiceTaskParserTest {
    private val parser = VoiceTaskParser(testLexicon())
    private val now = LocalDateTime.of(2026, 9, 22, 10, 15)

    @Test
    fun `parses Nicaraguan Spanish task with deadline and category`() {
        assertEquals(
            VoiceTaskDraft(
                title = "Comprar leche",
                category = Category.SHOPPING,
                dueDate = LocalDate.of(2026, 9, 23),
                dueTime = LocalTime.of(18, 0),
            ),
            parser.parse(
                "Recuérdame comprar leche mañana a las seis de la tarde categoría compras",
                now,
            ),
        )
    }

    @Test
    fun `parses next weekday numeric time and work category`() {
        assertEquals(
            VoiceTaskDraft(
                title = "Llamar a Ana",
                category = Category.WORK,
                dueDate = LocalDate.of(2026, 9, 28),
                dueTime = LocalTime.of(8, 30),
            ),
            parser.parse(
                "Crear tarea llamar a Ana el lunes a las 8:30 de la mañana para trabajo",
                now,
            ),
        )
    }

    @Test
    fun `infers today for a future time and tomorrow for a past time`() {
        assertEquals(
            LocalDate.of(2026, 9, 22),
            parser.parse("Comprar pan a las 6 pm", now).dueDate,
        )
        assertEquals(
            LocalDate.of(2026, 9, 23),
            parser.parse("Tomar medicina a las 8 de la mañana", now).dueDate,
        )
        assertEquals(
            LocalDate.of(2026, 9, 22),
            parser.parse(
                "Tomar medicina a las 8 de la mañana",
                LocalDateTime.of(2026, 9, 22, 6, 0),
            ).dueDate,
        )
    }

    @Test
    fun `extracts explicit description without including it in title`() {
        assertEquals(
            VoiceTaskDraft(
                title = "Preparar informe",
                description = "Incluir ventas del mes",
                category = Category.WORK,
                dueDate = LocalDate.of(2026, 9, 23),
                dueTime = LocalTime.of(9, 0),
            ),
            parser.parse(
                "Preparar informe con descripción incluir ventas del mes, categoría trabajo, mañana a las nueve de la mañana",
                now,
            ),
        )
    }

    @Test
    fun `does not treat ordinary para phrase as category`() {
        val result = parser.parse("Comprar flores para mi mamá", now)

        assertEquals("Comprar flores para mi mamá", result.title)
        assertNull(result.category)
        assertNull(result.dueDate)
        assertNull(result.dueTime)
    }

    @Test
    fun `parses fractional time followed by afternoon period`() {
        val result = parser.parse(
            "Llamar a Ana a las seis y media de la tarde",
            now,
        )

        assertEquals("Llamar a Ana", result.title)
        assertEquals(LocalTime.of(18, 30), result.dueTime)
    }

    @Test
    fun `parses dotted afternoon meridiem`() {
        assertEquals(
            LocalTime.of(18, 0),
            parser.parse("Llamar a Ana a las 6 p. m.", now).dueTime,
        )
    }

    @Test
    fun `parses English task with date time and category`() {
        assertEquals(
            VoiceTaskDraft(
                title = "Buy milk",
                category = Category.SHOPPING,
                dueDate = LocalDate.of(2026, 9, 23),
                dueTime = LocalTime.of(18, 30),
            ),
            parser.parse(
                "Remind me to buy milk tomorrow at 6:30 in the afternoon for shopping",
                now,
            ),
        )
    }

    @Test
    fun `keeps English dictation when no structured metadata is present`() {
        assertEquals(
            "Call my sister",
            parser.parse("Call my sister", now).title,
        )
    }

    @Test
    fun `parses English description and compound hour`() {
        val result = parser.parse(
            "Create a task call mom with description ask about the trip at 21",
            now,
        )

        assertEquals("Call mom", result.title)
        assertEquals("Ask about the trip", result.description)
        assertEquals(LocalTime.of(21, 0), result.dueTime)
    }
}

private fun testLexicon() = VoiceTaskLexicon(
    leadingCommands = listOf(
        "recuérdame", "recordame", "recuérdame que", "remind me", "remind me to",
        "remember", "remember to", "crea una tarea", "crear una tarea", "crear tarea",
        "agregar tarea", "agrega una tarea", "create task", "create a task", "add task", "add a task",
        "anota", "anotar", "note", "note that",
    ),
    categoryLabels = mapOf(
        Category.WORK to listOf("categoría trabajo", "categoría laboral", "para trabajo", "para laboral", "category work", "for work"),
        Category.PERSONAL to listOf("categoría personal", "para personal", "category personal", "for personal"),
        Category.SHOPPING to listOf("categoría compra", "categoría compras", "para compra", "para compras", "category shopping", "for shopping"),
        Category.OTHER to listOf("categoría otro", "categoría otros", "para otro", "para otros", "category other", "for other"),
    ),
    descriptionLabels = listOf("con descripción", "descripción", "detalles", "with description", "description", "details"),
    relativeDateLabels = mapOf(
        0L to listOf("hoy", "today"),
        1L to listOf("mañana", "manana", "tomorrow"),
        2L to listOf("pasado mañana", "day after tomorrow"),
    ),
    weekdayLabels = mapOf(
        DayOfWeek.MONDAY to listOf("lunes", "el lunes", "próximo lunes", "monday", "next monday"),
        DayOfWeek.TUESDAY to listOf("martes", "el martes", "próximo martes", "tuesday", "next tuesday"),
        DayOfWeek.WEDNESDAY to listOf("miércoles", "miercoles", "el miércoles", "wednesday", "next wednesday"),
        DayOfWeek.THURSDAY to listOf("jueves", "el jueves", "próximo jueves", "thursday", "next thursday"),
        DayOfWeek.FRIDAY to listOf("viernes", "el viernes", "próximo viernes", "friday", "next friday"),
        DayOfWeek.SATURDAY to listOf("sábado", "sabado", "el sábado", "saturday", "next saturday"),
        DayOfWeek.SUNDAY to listOf("domingo", "el domingo", "próximo domingo", "sunday", "next sunday"),
    ),
    timePrefixes = listOf("a la", "a las", "at"),
    morningMarkers = listOf("a. m.", "am", "de la mañana", "in the morning"),
    afternoonMarkers = listOf("p. m.", "pm", "de la tarde", "de la noche", "in the afternoon", "in the evening", "at night"),
    fractionMinutes = mapOf(30 to listOf("y media", "half past"), 15 to listOf("y cuarto", "quarter past")),
    hourWords = (0..23).associateWith { hour ->
        listOf(
            when (hour) {
                0 -> "cero"; 1 -> "una"; 2 -> "dos"; 3 -> "tres"; 4 -> "cuatro"; 5 -> "cinco"
                6 -> "seis"; 7 -> "siete"; 8 -> "ocho"; 9 -> "nueve"; 10 -> "diez"; 11 -> "once"
                12 -> "doce"; 13 -> "trece"; 14 -> "catorce"; 15 -> "quince"; 16 -> "dieciséis"
                17 -> "diecisiete"; 18 -> "dieciocho"; 19 -> "diecinueve"; 20 -> "veinte"
                21 -> "veintiuno"; 22 -> "veintidós"; else -> "veintitrés"
            },
        )
    },
)
