package com.manuelduarte077.notyapp.features.notes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class VoiceTaskParserTest {
    private val parser = VoiceTaskParser()
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
}
