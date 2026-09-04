package com.haitranduc.fittrack.presentation.exercise

import androidx.lifecycle.SavedStateHandle
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.testing.FakeExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeExerciseRepository

    private val sampleExercise = Exercise(
        id = "ex_bench",
        name = "Barbell Bench Press",
        bodyPart = "Chest",
        equipment = "Barbell",
        target = "Pectorals",
        muscleGroup = "Chest",
        secondaryMuscles = listOf("Triceps", "Anterior Deltoids"),
        instructions = listOf("Lie on bench", "Lower barbell", "Press up")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeExerciseRepository()
        repository.exercisesFlow.value = listOf(sampleExercise)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_withValidId_loadsExercise() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "ex_bench"))
        val viewModel = ExerciseDetailViewModel(repository, savedStateHandle)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.exercise)
        assertEquals("Barbell Bench Press", state.exercise?.name)
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun init_withNonExistentId_reachesMissingState() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "non_existent"))
        val viewModel = ExerciseDetailViewModel(repository, savedStateHandle)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.exercise)
        assertTrue(state.isMissing)
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun repositoryFailure_surfacesErrorMessage() = runTest(testDispatcher) {
        repository.returnDataFailure = true
        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "ex_bench"))
        val viewModel = ExerciseDetailViewModel(repository, savedStateHandle)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.exercise)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun retry_afterFailure_resubscribesAndLoadsExercise() = runTest(testDispatcher) {
        repository.returnDataFailure = true
        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "ex_bench"))
        val viewModel = ExerciseDetailViewModel(repository, savedStateHandle)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initialCount = repository.observeExerciseSubscriptionCount
        assertTrue(initialCount >= 1)
        val errorState = viewModel.uiState.value
        assertEquals(UiText.StringResource(R.string.error_database), errorState.errorMessage)
        assertNull(errorState.exercise)

        // Clear failure flag and retry
        repository.returnDataFailure = false
        viewModel.retry()
        advanceUntilIdle()

        // Subscription count increased
        assertTrue(repository.observeExerciseSubscriptionCount > initialCount)
        val successState = viewModel.uiState.value
        assertNull(successState.errorMessage)
        assertNotNull(successState.exercise)
        assertEquals("Barbell Bench Press", successState.exercise?.name)

        collectJob.cancel()
    }
}
