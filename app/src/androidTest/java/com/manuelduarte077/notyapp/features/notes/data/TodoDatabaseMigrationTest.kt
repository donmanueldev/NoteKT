package com.manuelduarte077.notyapp.features.notes.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoDatabaseMigrationTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TodoDatabase::class.java,
    )

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationFrom1To2PreservesCreationDateAndAddsNullableDeadline() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            (0..3).forEach { category ->
                execSQL(
                    """
                    INSERT INTO notes (id, title, description, category, is_completed, date)
                    VALUES ('legacy-$category', 'Tarea anterior', NULL, $category, 0, 1740000000000)
                    """.trimIndent(),
                )
            }
            close()
        }

        val database = helper.runMigrationsAndValidate(TEST_DATABASE, 2, true)
        val cursor = database.query(
            "SELECT category, date, due_date, due_time FROM notes ORDER BY category",
        )

        cursor.use {
            var expectedCategory = 0
            while (it.moveToNext()) {
                assertEquals(expectedCategory++, it.getInt(0))
                assertEquals(1740000000000, it.getLong(1))
                assertTrue(it.isNull(2))
                assertTrue(it.isNull(3))
            }
            assertEquals(4, expectedCategory)
        }
        database.close()
    }

    private companion object {
        const val TEST_DATABASE = "voice-migration-test"
    }
}
