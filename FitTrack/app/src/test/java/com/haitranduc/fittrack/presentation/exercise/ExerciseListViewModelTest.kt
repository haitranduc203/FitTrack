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
import kotlinx.coroutines.test.advanceTimeBy
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
class ExerciseListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeExerciseRepository
    private lateinit var favoriteRepository: FakeFavoriteExerciseRepository

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
        favoriteRepository = FakeFavoriteExerciseRepository()
        repository.exercisesFlow.value = sampleExercises
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ExerciseListViewModel {
        return ExerciseListViewModel(repository, favoriteRepository)
    }

    @Test
    fun init_observesAllExercisesByDefault() = runTest(testDispatcher) {
        val viewModel = createViewModel()
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
        assertFalse(state.isFavoritesOnly)
        assertTrue(state.favoriteExerciseIds.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun onSearchQueryChanged_filtersExercises() = runTest(testDispatcher) {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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

    @Test
    fun favoritesFilter_whenEnabled_filtersToOnlyFavoriteExercises() = runTest(testDispatcher) {
        favoriteRepository.favoriteIdsFlow.value = setOf("ex_1", "ex_3")
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.exercises.size)
        assertEquals(setOf("ex_1", "ex_3"), viewModel.uiState.value.favoriteExerciseIds)

        viewModel.onFavoritesFilterToggled(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFavoritesOnly)
        assertEquals(2, state.exercises.size)
        assertEquals(listOf("ex_1", "ex_3"), state.exercises.map { it.id })

        collectJob.cancel()
    }

    @Test
    fun favoritesFilter_composesWithSearchAndBodyPartFilters() = runTest(testDispatcher) {
        favoriteRepository.favoriteIdsFlow.value = setOf("ex_1", "ex_3")
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onFavoritesFilterToggled(true)
        viewModel.onBodyPartSelected("Chest")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.exercises.size)
        assertEquals("ex_1", state.exercises[0].id)

        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_callsRepository_andUpdatesFavoriteIds() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.favoriteExerciseIds.contains("ex_2"))

        viewModel.onToggleFavorite("ex_2")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.favoriteExerciseIds.contains("ex_2"))
        assertEquals(1, favoriteRepository.setFavoriteCallCount)

        viewModel.onToggleFavorite("ex_2")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.favoriteExerciseIds.contains("ex_2"))
        assertEquals(2, favoriteRepository.setFavoriteCallCount)

        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_emitsSnackbarEvent() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        var receivedEvent: ExerciseListEvent? = null
        val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onToggleFavorite("ex_2")
        advanceUntilIdle()

        assertEquals(
            ExerciseListEvent.ShowSnackbar(UiText.StringResource(R.string.msg_favorite_added)),
            receivedEvent
        )

        eventJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_duplicateTapWhilePending_isProtected() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onToggleFavorite("ex_1")
        // Second call while first is in progress
        viewModel.onToggleFavorite("ex_1")
        advanceUntilIdle()

        assertEquals(1, favoriteRepository.setFavoriteCallCount)

        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_repositoryFailure_surfacesErrorMessage() = runTest(testDispatcher) {
        favoriteRepository.setFavoriteError = DataError.Database(RuntimeException("Disk error"))
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onToggleFavorite("ex_1")
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), viewModel.uiState.value.favoriteErrorMessage)
        assertTrue(viewModel.uiState.value.pendingFavoriteIds.isEmpty())

        viewModel.onClearFavoriteError()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.favoriteErrorMessage)

        collectJob.cancel()
    }

    @Test
    fun retryFailure_retainsPreviouslyLoadedExercises() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.exercises.size)

        repository.returnDataFailure = true
        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
        assertEquals(4, state.exercises.size)

        collectJob.cancel()
    }

    @Test
    fun init_restoresFiltersFromSavedStateHandle() = runTest(testDispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                ExerciseListViewModel.KEY_SEARCH_QUERY to "Bench",
                ExerciseListViewModel.KEY_BODY_PART to "chest",
                ExerciseListViewModel.KEY_EQUIPMENT to "barbell",
                ExerciseListViewModel.KEY_FAVORITES_ONLY to true
            )
        )
        val viewModel = ExerciseListViewModel(repository, favoriteRepository, handle)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Bench", state.searchQuery)
        assertEquals("chest", state.selectedBodyPart)
        assertEquals("barbell", state.selectedEquipment)
        assertTrue(state.isFavoritesOnly)

        collectJob.cancel()
    }

    @Test
    fun identicalFilters_doNotTriggerDuplicateRepositorySubscription() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initialSubs = repository.observeExercisesSubscriptionCount
        assertTrue("Expected initial subscription", initialSubs >= 1)

        // 1. Same query with whitespace difference
        viewModel.onSearchQueryChanged("bench")
        advanceTimeBy(250)
        val afterQuerySubs = repository.observeExercisesSubscriptionCount
        assertEquals(initialSubs + 1, afterQuerySubs)

        viewModel.onSearchQueryChanged("  bench  ")
        advanceTimeBy(250)
        // Subscription count must NOT increase because normalized query is identical
        assertEquals(afterQuerySubs, repository.observeExercisesSubscriptionCount)

        // 2. Same body part with whitespace / identical value
        viewModel.onBodyPartSelected("Chest")
        advanceTimeBy(250)
        val afterBodyPartSubs = repository.observeExercisesSubscriptionCount
        assertEquals(afterQuerySubs + 1, afterBodyPartSubs)

        viewModel.onBodyPartSelected("  Chest  ")
        advanceTimeBy(250)
        assertEquals(afterBodyPartSubs, repository.observeExercisesSubscriptionCount)

        // 3. Same equipment
        viewModel.onEquipmentSelected("Barbell")
        advanceTimeBy(250)
        val afterEquipmentSubs = repository.observeExercisesSubscriptionCount
        assertEquals(afterBodyPartSubs + 1, afterEquipmentSubs)

        viewModel.onEquipmentSelected("  Barbell  ")
        advanceTimeBy(250)
        assertEquals(afterEquipmentSubs, repository.observeExercisesSubscriptionCount)

        // 4. Same favorites filter toggle
        viewModel.onFavoritesFilterToggled(true)
        advanceTimeBy(250)
        val afterFavSubs = repository.observeExercisesSubscriptionCount
        assertEquals(afterEquipmentSubs + 1, afterFavSubs)

        viewModel.onFavoritesFilterToggled(true)
        advanceTimeBy(250)
        assertEquals(afterFavSubs, repository.observeExercisesSubscriptionCount)

        collectJob.cancel()
    }

    @Test
    fun rapidQueryTyping_cancelsIntermediateDebouncedQueries() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initialSubs = repository.observeExercisesSubscriptionCount

        // User types rapidly within debounce window (200ms)
        viewModel.onSearchQueryChanged("b")
        advanceTimeBy(50)
        viewModel.onSearchQueryChanged("be")
        advanceTimeBy(50)
        viewModel.onSearchQueryChanged("ben")
        advanceTimeBy(50)
        viewModel.onSearchQueryChanged("bench")
        // Now let debounce settle
        advanceTimeBy(250)

        // Only 1 additional subscription should be made for "bench", not 4
        assertEquals(initialSubs + 1, repository.observeExercisesSubscriptionCount)
        assertEquals(1, viewModel.uiState.value.exercises.size)
        assertEquals("Bench Press", viewModel.uiState.value.exercises[0].name)

        collectJob.cancel()
    }

    @Test
    fun performanceBenchmark_with1324Exercises_filtersWithinBound() = runTest(testDispatcher) {
        val bodyParts = listOf("chest", "back", "legs", "shoulders", "arms", "waist", "cardio")
        val equipments = listOf("barbell", "dumbbell", "bodyweight", "cable", "machine")
        val exercises1324 = (1..1324).map { i ->
            val bp = bodyParts[i % bodyParts.size]
            val eq = equipments[i % equipments.size]
            Exercise(
                id = "ex_$i",
                name = "Exercise $i $bp $eq",
                bodyPart = bp,
                equipment = eq,
                target = "Target muscle $i",
                muscleGroup = bp,
                secondaryMuscles = listOf("Secondary $i"),
                instructions = listOf("Step 1 for $i", "Step 2 for $i")
            )
        }
        repository.setExercises(exercises1324)

        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(1324, viewModel.uiState.value.exercises.size)

        val startNs = System.nanoTime()

        // 1. Filter by query
        viewModel.onSearchQueryChanged("barbell")
        advanceTimeBy(250)
        val barbellCount = viewModel.uiState.value.exercises.size
        assertTrue("Should have barbell exercises", barbellCount > 0)

        // 2. Filter by bodyPart
        viewModel.onBodyPartSelected("chest")
        advanceTimeBy(250)
        val chestBarbellCount = viewModel.uiState.value.exercises.size
        assertTrue("Chest barbell exercises should be filtered", chestBarbellCount in 1 until barbellCount)

        // 3. Clear query, filter by equipment
        viewModel.onSearchQueryChanged("")
        viewModel.onEquipmentSelected("dumbbell")
        advanceTimeBy(250)
        assertTrue(viewModel.uiState.value.exercises.isNotEmpty())

        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        // Entire cycle of filtering across 1,324 items must finish well under 2,000ms
        assertTrue("Benchmark duration ($durationMs ms) exceeded bound", durationMs < 2000)

        collectJob.cancel()
    }
}
