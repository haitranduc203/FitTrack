package com.haitranduc.fittrack.presentation.workout

import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.testing.FakeWorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
class WorkoutListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutRepository: FakeWorkoutRepository

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
        val viewModel = WorkoutListViewModel(workoutRepository)
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
        val viewModel = WorkoutListViewModel(workoutRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.workouts.isEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun observationFailure_showsError_andRetryRecovers() = runTest {
        workoutRepository.observeError = DataError.Database(RuntimeException("Database unreachable"))
        val viewModel = WorkoutListViewModel(workoutRepository)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)

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
        val viewModel = WorkoutListViewModel(workoutRepository)
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
        val viewModel = WorkoutListViewModel(workoutRepository)
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
    fun deleteFailure_setsErrorMessage_andResetsDeleting() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1))
        workoutRepository.deleteError = DataError.Database(RuntimeException("Foreign key constraint"))
        val viewModel = WorkoutListViewModel(workoutRepository)
        advanceUntilIdle()

        viewModel.onDeleteRequested(sampleWorkout1)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertNotNull(state.errorMessage)
        // Workout not removed
        assertEquals(1, state.workouts.size)
    }

    @Test
    fun doubleDelete_preventsDuplicateCall() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout1))
        val viewModel = WorkoutListViewModel(workoutRepository)
        advanceUntilIdle()

        viewModel.onDeleteRequested(sampleWorkout1)
        viewModel.onDeleteConfirmed()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleting)
        assertTrue(viewModel.uiState.value.workouts.isEmpty())
    }

    private suspend fun kotlinx.coroutines.flow.Flow<com.haitranduc.fittrack.domain.repository.DataResult<List<Workout>>>.firstSuccessData(): List<Workout> {
        val result = this.first()
        return (result as com.haitranduc.fittrack.domain.repository.DataResult.Success<List<Workout>>).data
    }
}

