package com.manuelduarte077.notyapp.features.notes.presentation.detail

import android.annotation.SuppressLint
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.manuelduarte077.notyapp.R
import com.manuelduarte077.notyapp.analytics.AnalyticsTracker
import com.manuelduarte077.notyapp.features.notes.domain.Category
import com.manuelduarte077.notyapp.features.notes.domain.Task
import com.manuelduarte077.notyapp.features.notes.domain.TaskLocalDataSource
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognitionCandidate
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognitionEvent
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognitionEventListener
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognitionRequest
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognizer
import com.manuelduarte077.notyapp.features.notes.presentation.detail.voice.VoiceRecognizerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class TaskVoiceInputTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val permissionRegistry = AutoGrantPermissionRegistry()
    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = permissionRegistry
    }
    private val recognizer = FakeVoiceRecognizer()
    private val recognizerFactory = VoiceRecognizerFactory { recognizer }
    private val dataSource = RecordingTaskDataSource()
    private val analytics = RecordingAnalytics()
    private val viewModels = ViewModelStore()
    private var viewModelCount = 0
    private var backNavigations = 0

    @After
    fun clearViewModels() {
        compose.runOnIdle { viewModels.clear() }
    }

    @Test
    fun partialSpeechBecomesEditableTitleAndSavesOnce() {
        val viewModel = createViewModel(startVoiceInput = true)
        showTask(viewModel)

        compose.runOnIdle {
            assertEquals(
                context.resources.configuration.locales[0].toLanguageTag(),
                recognizer.requests.single().preferredLanguageTags.single(),
            )
            recognizer.emit(VoiceRecognitionEvent.PartialResult("comprar leche mañana"))
        }
        compose.onNodeWithText("comprar leche mañana").assertExists()

        compose.runOnIdle {
            recognizer.emit(
                VoiceRecognitionEvent.FinalResult(
                    listOf(
                        VoiceRecognitionCandidate(
                            "Recuérdame comprar leche mañana a las seis de la tarde categoría compras",
                            0.94f,
                        ),
                    ),
                ),
            )
            assertEquals(
                "Recuérdame comprar leche mañana a las seis de la tarde categoría compras",
                viewModel.state.taskName.text.toString(),
            )
            assertNull(viewModel.state.category)
            assertNull(viewModel.state.dueDate)
            assertNull(viewModel.state.dueTime)
            assertTrue(dataSource.inserted.isEmpty())
        }

        compose.onNodeWithText(context.getString(R.string.save)).performClick()
        compose.runOnIdle {
            assertEquals(1, dataSource.inserted.size)
            assertEquals(
                "Recuérdame comprar leche mañana a las seis de la tarde categoría compras",
                dataSource.inserted.single().title,
            )
            assertNull(dataSource.inserted.single().category)
            assertNull(dataSource.inserted.single().dueTime)
            assertEquals(1, backNavigations)
        }
    }

    @Test
    fun alternativesCanReplaceTheBestCandidateBeforeSaving() {
        val viewModel = createViewModel(startVoiceInput = true)
        compose.runOnIdle { populateDraft(viewModel) }
        showTask(viewModel)

        compose.runOnIdle {
            recognizer.emit(
                VoiceRecognitionEvent.FinalResult(
                    listOf(
                        VoiceRecognitionCandidate(
                            "Comprar queso mañana categoría compras",
                            0.62f,
                        ),
                        VoiceRecognitionCandidate("Comprar hueso", 0.51f),
                    ),
                ),
            )
        }
        compose.onAllNodes(hasSetTextAction())[0]
            .assertTextEquals("Comprar queso mañana categoría compras")
        compose.runOnIdle {
            assertEquals(Category.WORK, viewModel.state.category)
            assertEquals(LocalDate.of(2026, 9, 30), viewModel.state.dueDate)
        }
        compose.onNodeWithText(context.getString(R.string.voice_input_alternatives, 2)).performClick()
        compose.onNodeWithText("Comprar hueso (51%)").performClick()
        compose.onAllNodes(hasSetTextAction())[0].assertTextEquals("Comprar hueso")
        compose.runOnIdle {
            assertEquals("Detalles pendientes", viewModel.state.taskDescription.text.toString())
            assertEquals(Category.WORK, viewModel.state.category)
            assertEquals(LocalDate.of(2026, 9, 30), viewModel.state.dueDate)
            assertEquals(LocalTime.of(16, 30), viewModel.state.dueTime)
        }
    }

    @Test
    fun secondDictationReplacesOnlyTheTitle() {
        val viewModel = createViewModel(startVoiceInput = true)
        compose.runOnIdle { populateDraft(viewModel) }
        showTask(viewModel)

        compose.runOnIdle {
            recognizer.emit(
                VoiceRecognitionEvent.FinalResult(
                    listOf(
                        VoiceRecognitionCandidate(
                            "Comprar leche mañana a las seis de la tarde categoría compras",
                            0.9f,
                        ),
                    ),
                ),
            )
        }
        microphone().performClick()
        compose.runOnIdle {
            recognizer.emit(
                VoiceRecognitionEvent.FinalResult(
                    listOf(VoiceRecognitionCandidate("Llamar a mamá", 0.95f)),
                ),
            )
            assertEquals("Llamar a mamá", viewModel.state.taskName.text.toString())
            assertEquals("Detalles pendientes", viewModel.state.taskDescription.text.toString())
            assertEquals(Category.WORK, viewModel.state.category)
            assertEquals(LocalDate.of(2026, 9, 30), viewModel.state.dueDate)
            assertEquals(LocalTime.of(16, 30), viewModel.state.dueTime)
        }
    }

    @Test
    fun cancelAndLateRecognizerCallbackPreserveDraft() {
        val viewModel = createViewModel()
        compose.runOnIdle { populateDraft(viewModel) }
        showTask(viewModel)

        microphone().performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.stop_voice_input)).performClick()
        compose.runOnIdle {
            recognizer.emit(
                VoiceRecognitionEvent.FinalResult(
                    listOf(VoiceRecognitionCandidate("No aplicar", 1f)),
                ),
            )
            assertDraft(viewModel)
            assertTrue(dataSource.inserted.isEmpty())
        }
        microphone().assertIsEnabled()
    }

    @Test
    fun initialVoiceRequestIsConsumedOnceAndStaysConsumedAfterRestoration() {
        compose.runOnIdle {
            val handle = routeHandle(startVoiceInput = true)
            val viewModel = newViewModel(handle)
            assertTrue(viewModel.consumeInitialVoiceInput())
            assertFalse(viewModel.consumeInitialVoiceInput())
            assertFalse(newViewModel(restoreHandle(handle)).consumeInitialVoiceInput())
            assertFalse(newViewModel(routeHandle()).consumeInitialVoiceInput())
            assertFalse(
                newViewModel(routeHandle(taskId = "existing-task", startVoiceInput = true))
                    .consumeInitialVoiceInput(),
            )
        }
    }

    @Test
    fun structuredDraftRestoresAllFieldsAfterViewModelRecreation() {
        compose.runOnIdle {
            val handle = routeHandle()
            val original = newViewModel(handle)
            populateDraft(original)

            val restored = newViewModel(restoreHandle(handle))

            assertDraft(restored)
        }
    }

    @Test
    fun clearingDueDateAlsoClearsDueTime() {
        val viewModel = createViewModel()
        compose.runOnIdle {
            populateDraft(viewModel)
            viewModel.onAction(ActionTask.ChangeTaskDueDate(null))

            assertNull(viewModel.state.dueDate)
            assertNull(viewModel.state.dueTime)
        }
    }

    @Test
    fun whitespaceOnlyTitleCannotSaveFromUiOrAction() {
        val viewModel = createViewModel()
        showTask(viewModel)

        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("   ")
        compose.onNodeWithText(context.getString(R.string.save)).assertIsNotEnabled()
        compose.runOnIdle {
            viewModel.onAction(ActionTask.SaveTask)
            assertFalse(viewModel.state.isSaving)
            assertTrue(dataSource.inserted.isEmpty())
        }
    }

    @Test
    fun storageFailurePreservesStructuredDraftAndRetrySavesOnce() {
        val viewModel = createViewModel()
        compose.runOnIdle {
            populateDraft(viewModel)
            dataSource.addFailure = IllegalStateException("Storage unavailable")
        }
        showTask(viewModel)

        compose.onNodeWithText(context.getString(R.string.save)).performClick()
        compose.runOnIdle {
            assertDraft(viewModel)
            assertFalse(viewModel.state.isSaving)
            assertTrue(dataSource.inserted.isEmpty())
            dataSource.addFailure = null
        }

        compose.onNodeWithText(context.getString(R.string.save)).performClick()
        compose.runOnIdle {
            assertEquals(2, dataSource.addAttempts)
            assertEquals(1, dataSource.inserted.size)
            assertEquals(LocalDate.of(2026, 9, 30), dataSource.inserted.single().dueDate)
            assertEquals(LocalTime.of(16, 30), dataSource.inserted.single().dueTime)
            assertEquals(1, backNavigations)
        }
    }

    @Test
    fun editingExistingTaskPreservesCreationDateAndUpdatesDueFields() {
        val createdAt = LocalDateTime.of(2025, 2, 3, 10, 15)
        val existing = Task(
            id = "existing-task",
            title = "Título guardado",
            description = "Descripción guardada",
            isCompleted = true,
            category = Category.PERSONAL,
            date = createdAt,
            dueDate = LocalDate.of(2026, 10, 1),
            dueTime = LocalTime.of(9, 0),
        )
        dataSource.existingTask = existing
        val viewModel = createViewModel(
            handle = routeHandle(taskId = existing.id, startVoiceInput = true),
        )
        showTask(viewModel)

        compose.runOnIdle {
            viewModel.onAction(ActionTask.ChangeTaskDueTime(LocalTime.of(11, 30)))
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Título editado")
        compose.onNodeWithText(context.getString(R.string.save)).performClick()

        compose.runOnIdle {
            assertEquals(1, dataSource.updated.size)
            assertEquals(createdAt, dataSource.updated.single().date)
            assertEquals(LocalDate.of(2026, 10, 1), dataSource.updated.single().dueDate)
            assertEquals(LocalTime.of(11, 30), dataSource.updated.single().dueTime)
            assertTrue(recognizer.requests.isEmpty())
        }
    }

    private fun microphone() =
        compose.onNodeWithContentDescription(context.getString(R.string.dictate_task_title))

    private fun showTask(viewModel: TaskViewModel) {
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                MaterialTheme {
                    TaskScreenRoot(
                        viewModel = viewModel,
                        navigateBack = { backNavigations++; true },
                        voiceRecognizerFactory = recognizerFactory,
                    )
                }
            }
        }
    }

    private fun createViewModel(
        startVoiceInput: Boolean = false,
        handle: SavedStateHandle = routeHandle(startVoiceInput = startVoiceInput),
    ): TaskViewModel = compose.runOnIdle { newViewModel(handle) }

    private fun newViewModel(handle: SavedStateHandle): TaskViewModel =
        TaskViewModel(handle, dataSource, analytics).also {
            viewModels.put("task-${viewModelCount++}", it)
        }

    private fun routeHandle(taskId: String? = null, startVoiceInput: Boolean = false) =
        SavedStateHandle(mapOf("taskId" to taskId, "startVoiceInput" to startVoiceInput))

    @SuppressLint("RestrictedApi")
    private fun restoreHandle(handle: SavedStateHandle): SavedStateHandle =
        SavedStateHandle.createHandle(handle.savedStateProvider().saveState(), null)

    private fun populateDraft(viewModel: TaskViewModel) {
        viewModel.state.taskName.setTextAndPlaceCursorAtEnd("Título original")
        viewModel.state.taskDescription.setTextAndPlaceCursorAtEnd("Detalles pendientes")
        viewModel.onAction(ActionTask.ChangeTaskCategory(Category.WORK))
        viewModel.onAction(ActionTask.ChangeTaskDone(true))
        viewModel.onAction(ActionTask.ChangeTaskDueDate(LocalDate.of(2026, 9, 30)))
        viewModel.onAction(ActionTask.ChangeTaskDueTime(LocalTime.of(16, 30)))
    }

    private fun assertDraft(viewModel: TaskViewModel) {
        assertEquals("Título original", viewModel.state.taskName.text.toString())
        assertEquals("Detalles pendientes", viewModel.state.taskDescription.text.toString())
        assertEquals(Category.WORK, viewModel.state.category)
        assertEquals(LocalDate.of(2026, 9, 30), viewModel.state.dueDate)
        assertEquals(LocalTime.of(16, 30), viewModel.state.dueTime)
        assertTrue(viewModel.state.isTaskDone)
    }

    private class AutoGrantPermissionRegistry : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            if (contract is ActivityResultContracts.RequestPermission) {
                @Suppress("UNCHECKED_CAST")
                dispatchResult(requestCode, true as O)
            } else {
                error("Unexpected activity result contract: ${contract::class.java.name}")
            }
        }
    }

    private class FakeVoiceRecognizer : VoiceRecognizer {
        val requests = mutableListOf<VoiceRecognitionRequest>()
        private var listener: VoiceRecognitionEventListener? = null

        override fun start(
            request: VoiceRecognitionRequest,
            listener: VoiceRecognitionEventListener,
        ) {
            requests += request
            this.listener = listener
            listener.onEvent(VoiceRecognitionEvent.Ready)
        }

        fun emit(event: VoiceRecognitionEvent) {
            listener?.onEvent(event)
            if (event is VoiceRecognitionEvent.FinalResult ||
                event == VoiceRecognitionEvent.Empty ||
                event == VoiceRecognitionEvent.Failed
            ) {
                listener = null
            }
        }

        override fun stop() = Unit

        override fun cancel() {
            listener = null
        }

        override fun close() {
            listener = null
        }
    }

    private class RecordingTaskDataSource : TaskLocalDataSource {
        val inserted = mutableListOf<Task>()
        val updated = mutableListOf<Task>()
        var existingTask: Task? = null
        var addFailure: RuntimeException? = null
        var addAttempts = 0
        override val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

        override suspend fun addTask(task: Task) {
            addAttempts++
            addFailure?.let { throw it }
            inserted += task
        }

        override suspend fun getTaskById(taskId: String): Task? =
            existingTask?.takeIf { it.id == taskId }

        override suspend fun updateTask(updatedTask: Task) {
            updated += updatedTask
        }

        override suspend fun removeTask(task: Task) = error("Unexpected delete")
        override suspend fun deleteAllTasks() = error("Unexpected delete")
        override suspend fun removeAllTasks() = error("Unexpected delete")
    }

    private class RecordingAnalytics : AnalyticsTracker {
        val events = mutableListOf<Pair<String, Map<String, String>>>()
        override fun logEvent(name: String, parameters: Map<String, String>) {
            events += name to parameters
        }

        override fun logScreenView(screenName: String) = Unit
    }
}
