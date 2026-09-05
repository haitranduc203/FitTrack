package com.haitranduc.fittrack.presentation.history

import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.testing.FakeStatisticsRepository
import com.haitranduc.fittrack.testing.FakeWorkoutHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: FakeWorkoutHistoryRepository
    private lateinit var statisticsRepository: FakeStatisticsRepository

    private val sampleSession1 = WorkoutSession(
        id = 1L,
        workoutId = 100L,
        workoutNameSnapshot = "Push Day",
        startedAt = 1000L,
        finishedAt = 2000L,
        durationSeconds = 1000L,
        sets = emptyList()
    )

    private val sampleSession2 = WorkoutSession(
        id = 2L,
        workoutId = null, // Template was deleted
        workoutNameSnapshot = "Deleted Template Workout",
        startedAt = 3000L,
        finishedAt = 4500L,
        durationSeconds = 1500L,
        sets = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepository = FakeWorkoutHistoryRepository()
        statisticsRepository = FakeStatisticsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observeHistory_empty_showsEmptySessions() = runTest {
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.sessions.isEmpty())
        assertEquals(0L, state.totalWorkouts)
    }

    @Test
    fun observeHistory_success_loadsFinishedSessionsIncludingDeletedTemplates() = runTest {
        historyRepository.sessions.addAll(listOf(sampleSession1, sampleSession2))
        historyRepository.refreshFlow()

        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.sessions.size)
        assertEquals("Push Day", state.sessions[0].workoutNameSnapshot)
        assertEquals("Deleted Template Workout", state.sessions[1].workoutNameSnapshot)
    }

    @Test
    fun observeHistory_failure_showsErrorMessage() = runTest {
        historyRepository.observeHistoryError = DataError.Database(RuntimeException("DB Read Error"))

        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
        assertTrue(state.sessions.isEmpty())
    }

    @Test
    fun observeTotalWorkouts_empty_showsZero() = runTest {
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0L, state.totalWorkouts)
    }

    @Test
    fun observeTotalWorkouts_success_updatesTotalWorkouts() = runTest {
        statisticsRepository.setTotalWorkouts(5L)
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5L, state.totalWorkouts)
    }

    @Test
    fun observeTotalWorkouts_failure_showsErrorMessage() = runTest {
        statisticsRepository.observeTotalWorkoutsError = DataError.Database(RuntimeException("DB Read Error"))
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
    }

    @Test
    fun observeTotalCompletedSets_empty_showsZero() = runTest {
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0L, state.totalCompletedSets)
    }

    @Test
    fun observeTotalCompletedSets_success_updatesTotalCompletedSets() = runTest {
        statisticsRepository.setTotalCompletedSets(24L)
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(24L, state.totalCompletedSets)
    }

    @Test
    fun observeTotalCompletedSets_failure_showsErrorMessage() = runTest {
        statisticsRepository.observeTotalCompletedSetsError = DataError.Database(RuntimeException("DB Read Error"))
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
    }

    @Test
    fun observeTotalTrainingTimeSeconds_empty_showsZero() = runTest {
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0L, state.totalTrainingTimeSeconds)
    }

    @Test
    fun observeTotalTrainingTimeSeconds_success_updatesTotalTrainingTime() = runTest {
        statisticsRepository.setTotalTrainingTimeSeconds(7500L)
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(7500L, state.totalTrainingTimeSeconds)
    }

    @Test
    fun observeTotalTrainingTimeSeconds_failure_showsErrorMessage() = runTest {
        statisticsRepository.observeTotalTrainingTimeSecondsError = DataError.Database(RuntimeException("DB Read Error"))
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
    }

    @Test
    fun formatTrainingTime_matchesRequirements() {
        // 0 seconds -> 0m
        assertEquals("0m", formatTrainingTime(0L))
        assertEquals("0m", formatTrainingTime(null))
        assertEquals("0m", formatTrainingTime(-10L))

        // 59 minutes -> 59m
        assertEquals("59m", formatTrainingTime(59L * 60L))

        // 60 minutes -> 1h 0m
        assertEquals("1h 0m", formatTrainingTime(60L * 60L))

        // 125 minutes -> 2h 5m
        assertEquals("2h 5m", formatTrainingTime(125L * 60L))
    }

    @Test
    fun retry_clearsErrorAndReloads() = runTest {
        historyRepository.observeHistoryError = DataError.Database(RuntimeException("DB Read Error"))
        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), viewModel.uiState.value.errorMessage)

        // Clear error and add data
        historyRepository.observeHistoryError = null
        historyRepository.sessions.add(sampleSession1)
        historyRepository.refreshFlow()

        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.sessions.size)
    }

    @Test
    fun observeHistory_refreshFailure_retainsPreviouslyLoadedSessions() = runTest {
        historyRepository.sessions.add(sampleSession1)
        historyRepository.refreshFlow()

        val viewModel = HistoryViewModel(historyRepository, statisticsRepository)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sessions.size)
        assertEquals("Push Day", viewModel.uiState.value.sessions[0].workoutNameSnapshot)

        // Simulate refresh failure
        historyRepository.observeHistoryError = DataError.Database(RuntimeException("Network failure"))
        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
        assertEquals(1, state.sessions.size)
        assertEquals("Push Day", state.sessions[0].workoutNameSnapshot)
    }
}
