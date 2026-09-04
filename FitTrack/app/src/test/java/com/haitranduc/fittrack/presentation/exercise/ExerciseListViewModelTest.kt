package com.haitranduc.fittrack.presentation.exercise

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeExerciseRepository

    private val sampleExercises = listOf(
        Exercise("ex_1", "Bench Press", "Chest", "Barbell", "Pectorals", "Chest", listOf("Triceps"), listOf("Step 1")),
        Exercise("ex_2", "Dumbbell Fly", "Chest", "Dumbbell", "Pectorals", "Chest", emptyList(), listOf("Step 1")),
        Exercise("ex_3", "Squat", "Legs", "Barbell", "Quadriceps", "Legs", listOf("Glutes"), listOf("Step 1")),
        Exercise("ex_4", "Pull Up", "Back", "Bodyweight", "Lats", "Back", listOf("Biceps"), listOf("Step 1"))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeExerciseRepository()
        repository.exercisesFlow.value = sampleExercises
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_observesAllExercisesByDefault() = runTest(testDispatcher) {
        val viewModel = ExerciseListViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.exercises.size)
        assertEquals("", state.searchQuery)
        assertNull(state.selectedBodyPart)
        assertNull(state.selectedEquipment)
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun onSearchQueryChanged_filtersExercises() = runTest(testDispatcher) {
        val viewModel = ExerciseListViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        viewModel.onSearchQueryChanged("  bench  ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("  bench  ", state.searchQuery)
        assertEquals(1, state.exercises.size)
        assertEquals("Bench Press", state.exercises[0].name)

        collectJob.cancel()
    }

    @Test
    fun onBodyPartSelected_andClearing_filtersCorrectly() = runTest(testDispatcher) {
        val viewModel = ExerciseListViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        viewModel.onBodyPartSelected("Chest")
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals("Chest", state.selectedBodyPart)
        assertEquals(2, state.exercises.size)

        // Clear filter
        viewModel.onBodyPartSelected(null)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertNull(state.selectedBodyPart)
        assertEquals(4, state.exercises.size)

        collectJob.cancel()
    }

    @Test
    fun onEquipmentSelected_andCombinedFilters_filterCorrectly() = runTest(testDispatcher) {
        val viewModel = ExerciseListViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        viewModel.onBodyPartSelected("Chest")
        viewModel.onEquipmentSelected("Dumbbell")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.exercises.size)
        assertEquals("Dumbbell Fly", state.exercises[0].name)

        collectJob.cancel()
    }

    @Test
    fun emptyResult_showsEmptyListWithoutError() = runTest(testDispatcher) {
        val viewModel = ExerciseListViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        viewModel.onSearchQueryChanged("NonExistentExerciseXYZ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.exercises.isEmpty())
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun repositoryFailure_surfacesErrorMessage() = runTest(testDispatcher) {
        repository.returnDataFailure = true
        val viewModel = ExerciseListViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun retry_afterFailure_resubscribesAndLoadsContent() = runTest(testDispatcher) {
        repository.returnDataFailure = true
        val viewModel = ExerciseListViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // 1. Initial state is Error
        val errorState = viewModel.uiState.value
        assertEquals(UiText.StringResource(R.string.error_database), errorState.errorMessage)
        assertTrue(errorState.exercises.isEmpty())
        val initialSubscriptionCount = repository.observeExercisesSubscriptionCount
        assertTrue(initialSubscriptionCount >= 1)

        // 2. Clear failure flag and retry
        repository.returnDataFailure = false
        viewModel.onRetry()
        advanceUntilIdle()

        // 3. Re-subscribed and loaded content, old error cleared
        assertTrue(repository.observeExercisesSubscriptionCount > initialSubscriptionCount)
        val successState = viewModel.uiState.value
        assertNull(successState.errorMessage)
        assertEquals(4, successState.exercises.size)

        collectJob.cancel()
    }
}
