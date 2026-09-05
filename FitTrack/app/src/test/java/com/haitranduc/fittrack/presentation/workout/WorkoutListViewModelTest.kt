package com.haitranduc.fittrack.presentation.workout

import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.presentation.util.UiText
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutRepository: FakeWorkoutRepository

    private fun createViewModel(): WorkoutListViewModel = WorkoutListViewModel(workoutRepository)

    private val sampleWorkout1 = Workout(
        id = 1L,
        name = "Push Day",
        createdAt = 1000L,
        updatedAt = 1000L,
        exercises = listOf(
            Exercise(
                id = "e1",
                name = "Bench Press",
                bodyPart = "chest",
                equipment = "barbell",
                target = "pectorals",
                muscleGroup = "chest",
                secondaryMuscles = emptyList(),
                instructions = emptyList()
            )
        )
    )

    private val sampleWorkout2 = Workout(
        id = 2L,
        name = "Pull Day",
        createdAt = 2000L,
        updatedAt = 2000L,
        exercises = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = FakeWorkoutRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsWorkouts() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1, sampleWorkout2))
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.workouts.size)
        assertEquals("Push Day", state.workouts[0].name)
        assertEquals("Pull Day", state.workouts[1].name)
    }

    @Test
    fun emptyWorkouts_showsEmptyState() = runTest {
        workoutRepository.setWorkouts(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.workouts.isEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun observationFailure_showsError_andRetryRecovers() = runTest {
        workoutRepository.observeError = DataError.Database(RuntimeException("Database unreachable"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)

        // Clear error and retry
        workoutRepository.observeError = null
        workoutRepository.setWorkouts(listOf(sampleWorkout1))
        viewModel.retry()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.workouts.size)
    }

    @Test
    fun deleteConfirmation_openAndDismiss() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDeleteRequested(sampleWorkout1)
        assertEquals(sampleWorkout1, viewModel.uiState.value.workoutToDelete)

        viewModel.onDeleteDismissed()
        assertNull(viewModel.uiState.value.workoutToDelete)
        // Ensure not deleted
        assertEquals(1, workoutRepository.observeWorkouts().firstSuccessData().size)
    }

    @Test
    fun deleteConfirmed_deletesFromRepository() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1, sampleWorkout2))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDeleteRequested(sampleWorkout1)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.workoutToDelete)
        assertFalse(state.isDeleting)
        assertNull(state.errorMessage)
        assertEquals(1, state.workouts.size)
        assertEquals(2L, state.workouts[0].id)
    }

    @Test
    fun deleteConfirmed_emitsSnackbarEvent() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1))
        val viewModel = createViewModel()
        advanceUntilIdle()

        var receivedEvent: WorkoutListEvent? = null
        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onDeleteRequested(sampleWorkout1)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(WorkoutListEvent.ShowSnackbar(UiText.StringResource(R.string.msg_workout_deleted)), receivedEvent)
        job.cancel()
    }

    @Test
    fun deleteFailure_setsErrorMessage_andResetsDeleting() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1))
        workoutRepository.deleteError = DataError.Database(RuntimeException("Foreign key constraint"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDeleteRequested(sampleWorkout1)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
        // Workout not removed
        assertEquals(1, state.workouts.size)
    }

    @Test
    fun doubleDelete_preventsDuplicateCall() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDeleteRequested(sampleWorkout1)
        viewModel.onDeleteConfirmed()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleting)
        assertTrue(viewModel.uiState.value.workouts.isEmpty())
    }

    @Test
    fun observationFailure_retainsPreviouslyLoadedWorkouts() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1))
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.workouts.size)

        workoutRepository.observeError = DataError.Database(RuntimeException("Intermittent failure"))
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
        assertEquals(1, state.workouts.size)
        assertEquals("Push Day", state.workouts[0].name)
    }

    @Test
    fun workoutSaved_recreationDoesNotReplaySnackbar() = runTest {
        val vm1 = createViewModel()
        advanceUntilIdle()

        val events1 = mutableListOf<WorkoutListEvent>()
        val job1 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm1.events.toList(events1)
        }
        vm1.onWorkoutSavedResult()
        advanceUntilIdle()
        assertEquals(1, events1.size)
        job1.cancel()

        // A recreated ViewModel does not replay a previously consumed UI event.
        val vm2 = createViewModel()
        advanceUntilIdle()

        val events2 = mutableListOf<WorkoutListEvent>()
        val job2 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm2.events.toList(events2)
        }
        advanceUntilIdle()
        assertTrue(events2.isEmpty())
        job2.cancel()
    }

    @Test
    fun workoutSavedResult_emitsSingleSnackbar() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<WorkoutListEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.onWorkoutSavedResult()
        advanceUntilIdle()

        assertEquals(
            listOf(WorkoutListEvent.ShowSnackbar(UiText.StringResource(R.string.msg_workout_saved))),
            events
        )
        job.cancel()
    }

    private suspend fun kotlinx.coroutines.flow.Flow<com.haitranduc.fittrack.domain.repository.DataResult<List<Workout>>>.firstSuccessData(): List<Workout> {
        val result = this.first()
        return (result as com.haitranduc.fittrack.domain.repository.DataResult.Success<List<Workout>>).data
    }
}
