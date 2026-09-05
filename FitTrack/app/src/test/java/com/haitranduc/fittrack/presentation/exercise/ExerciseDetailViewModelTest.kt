package com.haitranduc.fittrack.presentation.exercise

import androidx.lifecycle.SavedStateHandle
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.testing.FakeExerciseRepository
import com.haitranduc.fittrack.testing.FakeFavoriteExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
class ExerciseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeExerciseRepository
    private lateinit var favoriteRepository: FakeFavoriteExerciseRepository

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
        favoriteRepository = FakeFavoriteExerciseRepository()
        repository.exercisesFlow.value = listOf(sampleExercise)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(exerciseId: String?): ExerciseDetailViewModel {
        val handle = if (exerciseId != null) SavedStateHandle(mapOf("exerciseId" to exerciseId)) else SavedStateHandle()
        return ExerciseDetailViewModel(repository, favoriteRepository, handle)
    }

    @Test
    fun init_withValidId_loadsExercise() = runTest(testDispatcher) {
        val viewModel = createViewModel("ex_bench")
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.exercise)
        assertEquals("Barbell Bench Press", state.exercise?.name)
        assertNull(state.errorMessage)
        assertEquals(false, state.isFavorite)

        collectJob.cancel()

    }

    @Test
    fun init_withNonExistentId_reachesMissingState() = runTest(testDispatcher) {
        val viewModel = createViewModel("non_existent")
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
        val viewModel = createViewModel("ex_bench")
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
        val viewModel = createViewModel("ex_bench")
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

    @Test
    fun init_observesFavoriteState_whenExerciseIsFavorite() = runTest(testDispatcher) {
        favoriteRepository.favoriteIdsFlow.value = setOf("ex_bench")
        val viewModel = createViewModel("ex_bench")
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isFavorite)

        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_togglesStateInRepository() = runTest(testDispatcher) {
        val viewModel = createViewModel("ex_bench")
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isFavorite)

        viewModel.onToggleFavorite()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isFavorite)
        assertEquals(1, favoriteRepository.setFavoriteCallCount)

        viewModel.onToggleFavorite()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isFavorite)
        assertEquals(2, favoriteRepository.setFavoriteCallCount)

        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_emitsSnackbarEvent() = runTest(testDispatcher) {
        val viewModel = createViewModel("ex_bench")
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        var receivedEvent: ExerciseDetailEvent? = null
        val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onToggleFavorite()
        advanceUntilIdle()

        assertEquals(
            ExerciseDetailEvent.ShowSnackbar(UiText.StringResource(R.string.msg_favorite_added)),
            receivedEvent
        )

        eventJob.cancel()
        collectJob.cancel()
    }


    @Test
    fun toggleFavorite_failure_surfacesErrorMessage() = runTest(testDispatcher) {
        favoriteRepository.setFavoriteError = DataError.Database(RuntimeException("Disk error"))
        val viewModel = createViewModel("ex_bench")
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onToggleFavorite()
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), viewModel.uiState.value.favoriteErrorMessage)
        assertFalse(viewModel.uiState.value.isTogglingFavorite)

        viewModel.onClearFavoriteError()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.favoriteErrorMessage)

        collectJob.cancel()
    }

    @Test
    fun favoriteObservationFailure_surfacesFavoriteError_andDoesNotConfirmNotFavorite() = runTest(testDispatcher) {
        favoriteRepository.errorToEmit = DataError.Database(RuntimeException("Favorite read failed"))
        val viewModel = createViewModel("ex_bench")
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.exercise)
        assertNull("Failed observation must not confirm not-favorite", state.isFavorite)
        assertEquals(UiText.StringResource(R.string.error_database), state.favoriteErrorMessage)

        collectJob.cancel()
    }

    @Test
    fun retry_afterFavoriteObservationFailure_recoversFavoriteState() = runTest(testDispatcher) {
        favoriteRepository.errorToEmit = DataError.Database(RuntimeException("Favorite read failed"))
        val viewModel = createViewModel("ex_bench")
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), viewModel.uiState.value.favoriteErrorMessage)
        assertNull(viewModel.uiState.value.isFavorite)

        // Clear error and retry
        favoriteRepository.errorToEmit = null
        favoriteRepository.favoriteIdsFlow.value = setOf("ex_bench")
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.favoriteErrorMessage)
        assertEquals(true, state.isFavorite)

        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_duplicateTaps_ignoresSubsequentTapsWhileUpdating() = runTest(testDispatcher) {
        val viewModel = createViewModel("ex_bench")
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Call toggle twice in the same tick before advanceUntilIdle
        viewModel.onToggleFavorite()
        viewModel.onToggleFavorite()
        advanceUntilIdle()

        assertEquals(1, favoriteRepository.setFavoriteCallCount)

        collectJob.cancel()
    }
}
