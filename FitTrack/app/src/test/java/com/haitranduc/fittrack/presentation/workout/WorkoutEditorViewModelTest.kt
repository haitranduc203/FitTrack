package com.haitranduc.fittrack.presentation.workout

import androidx.lifecycle.SavedStateHandle
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.usecase.SaveWorkoutUseCase
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.testing.FakeExerciseRepository
import com.haitranduc.fittrack.testing.FakeTimeProvider
import com.haitranduc.fittrack.testing.FakeWorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var workoutHistoryRepository: com.haitranduc.fittrack.testing.FakeWorkoutHistoryRepository
    private lateinit var exerciseRepository: FakeExerciseRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveWorkoutUseCase: SaveWorkoutUseCase
    private lateinit var startWorkoutUseCase: com.haitranduc.fittrack.domain.usecase.StartWorkoutUseCase

    private val exerciseA = Exercise(
        id = "e1",
        name = "Bench Press",
        bodyPart = "chest",
        equipment = "barbell",
        target = "pectorals",
        muscleGroup = "chest",
        secondaryMuscles = emptyList(),
        instructions = emptyList()
    )

    private val exerciseB = Exercise(
        id = "e2",
        name = "Incline Dumbbell Press",
        bodyPart = "chest",
        equipment = "dumbbell",
        target = "pectorals",
        muscleGroup = "chest",
        secondaryMuscles = emptyList(),
        instructions = emptyList()
    )

    private val exerciseC = Exercise(
        id = "e3",
        name = "Cable Fly",
        bodyPart = "chest",
        equipment = "cable",
        target = "pectorals",
        muscleGroup = "chest",
        secondaryMuscles = emptyList(),
        instructions = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = FakeWorkoutRepository()
        workoutHistoryRepository = com.haitranduc.fittrack.testing.FakeWorkoutHistoryRepository()
        exerciseRepository = FakeExerciseRepository()
        timeProvider = FakeTimeProvider(1000L)
        saveWorkoutUseCase = SaveWorkoutUseCase(workoutRepository, timeProvider)
        startWorkoutUseCase = com.haitranduc.fittrack.domain.usecase.StartWorkoutUseCase(
            workoutRepository = workoutRepository,
            workoutHistoryRepository = workoutHistoryRepository,
            timeProvider = timeProvider
        )

        exerciseRepository.setExercises(listOf(exerciseA, exerciseB, exerciseC))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(workoutId: String? = null): WorkoutEditorViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (workoutId != null) set("workoutId", workoutId)
        }
        return WorkoutEditorViewModel(
            savedStateHandle = savedStateHandle,
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            saveWorkoutUseCase = saveWorkoutUseCase,
            startWorkoutUseCase = startWorkoutUseCase
        )
    }

    @Test
    fun createMode_initialState_isEmptyAndReady() = runTest {
        val viewModel = createViewModel(workoutId = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isMissing)
        assertFalse(state.isSaving)
        assertNull(state.workoutId)
        assertEquals("", state.workoutName)
        assertTrue(state.exercises.isEmpty())
        assertFalse(state.isPickerOpen)
    }

    @Test
    fun editMode_loadsExistingWorkout() = runTest {
        val existing = Workout(
            id = 10L,
            name = "Push Day",
            createdAt = 500L,
            updatedAt = 600L,
            exercises = listOf(exerciseA, exerciseB)
        )
        workoutRepository.setWorkouts(listOf(existing))

        val viewModel = createViewModel(workoutId = "10")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isMissing)
        assertEquals(10L, state.workoutId)
        assertEquals("Push Day", state.workoutName)
        assertEquals(listOf(exerciseA, exerciseB), state.exercises)
    }

    @Test
    fun editMode_missingWorkout_setsIsMissing() = runTest {
        val viewModel = createViewModel(workoutId = "999")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isMissing)
    }

    @Test
    fun editMode_invalidWorkoutId_setsIsMissing() = runTest {
        val viewModel = createViewModel(workoutId = "invalid_id")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isMissing)
    }

    @Test
    fun onNameChanged_updatesStateAndClearsError() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNameChanged("Leg Day")
        assertEquals("Leg Day", viewModel.uiState.value.workoutName)
        assertNull(viewModel.uiState.value.nameErrorRes)
    }

    @Test
    fun exercisePicker_openCloseAndSearch() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openExercisePicker()
        assertTrue(viewModel.uiState.value.isPickerOpen)

        viewModel.onPickerQueryChanged("Incline")
        advanceUntilIdle()

        val pickerExercises = viewModel.uiState.value.pickerExercises
        assertEquals(1, pickerExercises.size)
        assertEquals("Incline Dumbbell Press", pickerExercises[0].name)

        viewModel.closeExercisePicker()
        assertFalse(viewModel.uiState.value.isPickerOpen)
    }

    @Test
    fun addExercise_appendsToList_andClosesPicker() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openExercisePicker()
        viewModel.onExerciseSelected(exerciseA)

        assertEquals(listOf(exerciseA), viewModel.uiState.value.exercises)
        assertFalse(viewModel.uiState.value.isPickerOpen)
    }

    @Test
    fun addExercise_duplicate_preventsAdding() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onExerciseSelected(exerciseA)
        viewModel.onExerciseSelected(exerciseA)

        assertEquals(1, viewModel.uiState.value.exercises.size)
        assertNotNull(viewModel.uiState.value.exerciseErrorRes)
    }

    @Test
    fun removeExercise_removesAtIndex() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onExerciseSelected(exerciseA)
        viewModel.onExerciseSelected(exerciseB)

        viewModel.onRemoveExercise(0)
        assertEquals(listOf(exerciseB), viewModel.uiState.value.exercises)
    }

    @Test
    fun reorderExercises_moveUpAndDown_withBoundaries() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onExerciseSelected(exerciseA)
        viewModel.onExerciseSelected(exerciseB)
        viewModel.onExerciseSelected(exerciseC)

        // Move top item up -> no change
        viewModel.onMoveExerciseUp(0)
        assertEquals(listOf(exerciseA, exerciseB, exerciseC), viewModel.uiState.value.exercises)

        // Move bottom item down -> no change
        viewModel.onMoveExerciseDown(2)
        assertEquals(listOf(exerciseA, exerciseB, exerciseC), viewModel.uiState.value.exercises)

        // Move middle item up
        viewModel.onMoveExerciseUp(1)
        assertEquals(listOf(exerciseB, exerciseA, exerciseC), viewModel.uiState.value.exercises)

        // Move top item down
        viewModel.onMoveExerciseDown(0)
        assertEquals(listOf(exerciseA, exerciseB, exerciseC), viewModel.uiState.value.exercises)
    }

    @Test
    fun save_invalidState_showsErrors_doesNotCallRepository() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Empty name and empty exercises
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.nameErrorRes)
        assertNotNull(viewModel.uiState.value.exerciseErrorRes)
        assertTrue(workoutRepository.savedWorkouts.isEmpty())
    }

    @Test
    fun save_success_emitsSavedEvent() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNameChanged("Chest Day")
        viewModel.onExerciseSelected(exerciseA)

        viewModel.onSaveClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(1, workoutRepository.savedWorkouts.size)

        val event = viewModel.events.first()
        assertTrue(event is WorkoutEditorEvent.NavigateBack)
        assertEquals(1L, (event as WorkoutEditorEvent.NavigateBack).workoutId)
    }

    @Test
    fun save_doubleClick_preventsDuplicateSave() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNameChanged("Chest Day")
        viewModel.onExerciseSelected(exerciseA)

        // Trigger two saves simultaneously
        viewModel.onSaveClicked()
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(1, workoutRepository.savedWorkouts.size)
    }

    @Test
    fun save_repositoryFailure_setsErrorMessage() = runTest {
        workoutRepository.saveError = DataError.Database(RuntimeException("DB error"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNameChanged("Chest Day")
        viewModel.onExerciseSelected(exerciseA)

        viewModel.onSaveClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
    }

    @Test
    fun onStartClicked_validChanges_savesFirstThenStartsWorkoutAndEmitsNavigateToActiveWorkout() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNameChanged("Leg Day")
        viewModel.onExerciseSelected(exerciseA)

        viewModel.onStartClicked()
        advanceUntilIdle()

        // 1. Saved in repository
        assertEquals(1, workoutRepository.savedWorkouts.size)
        val savedId = workoutRepository.savedWorkouts[0].id
        // 2. Session created in history repository
        assertEquals(1, workoutHistoryRepository.sessions.size)
        val session = workoutHistoryRepository.sessions[0]
        assertEquals(savedId, session.workoutId)
        // 3. Navigation event emitted
        val event = viewModel.events.first()
        assertTrue(event is WorkoutEditorEvent.NavigateToActiveWorkout)
        assertEquals(session.id, (event as WorkoutEditorEvent.NavigateToActiveWorkout).sessionId)
    }

    @Test
    fun onStartClicked_invalidChanges_showsValidationErrorWithoutSavingOrStarting() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Blank name, no exercises
        viewModel.onStartClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.nameErrorRes)
        assertNotNull(state.exerciseErrorRes)
        assertTrue(workoutRepository.savedWorkouts.isEmpty())
        assertTrue(workoutHistoryRepository.sessions.isEmpty())
    }

    @Test
    fun onStartClicked_activeSessionConflict_emitsNavigateToActiveWorkoutWithExistingSessionId() = runTest {
        // Pre-existing active session
        val existingSession = WorkoutSession(99L, 1L, "Push Day", 1000L, null, null, emptyList())
        workoutHistoryRepository.sessions.add(existingSession)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNameChanged("Pull Day")
        viewModel.onExerciseSelected(exerciseA)

        viewModel.onStartClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)

        // 1. Session in repository must NOT have increased (no new session inserted)
        assertEquals(1, workoutHistoryRepository.sessions.size)

        // 2. Navigation event must be emitted with existing session ID, exactly once
        val eventsReceived = mutableListOf<WorkoutEditorEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(eventsReceived)
        }
        assertEquals(listOf(WorkoutEditorEvent.NavigateToActiveWorkout(99L)), eventsReceived)
        advanceUntilIdle()
        assertEquals(1, eventsReceived.size)
        job.cancel()
    }
}
