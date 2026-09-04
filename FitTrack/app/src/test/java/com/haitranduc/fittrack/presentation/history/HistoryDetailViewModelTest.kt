package com.haitranduc.fittrack.presentation.history

import androidx.lifecycle.SavedStateHandle
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.presentation.util.UiText
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: FakeWorkoutHistoryRepository

    private val sampleSets = listOf(
        SetLog(id = 1L, sessionId = 10L, exerciseId = "e1", exerciseNameSnapshot = "Bench Press", setNumber = 1, reps = 10, weightKg = 60.0, completedAt = 1050L),
        SetLog(id = 2L, sessionId = 10L, exerciseId = "e1", exerciseNameSnapshot = "Bench Press", setNumber = 2, reps = 8, weightKg = 65.0, completedAt = 1150L)
    )

    private val sampleSession = WorkoutSession(
        id = 10L,
        workoutId = null, // Deleted template
        workoutNameSnapshot = "Push Day",
        startedAt = 1000L,
        finishedAt = 2000L,
        durationSeconds = 1000L,
        sets = sampleSets
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepository = FakeWorkoutHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(sessionId: Any? = 10L): HistoryDetailViewModel {
        val handle = if (sessionId != null) {
            SavedStateHandle(mapOf("sessionId" to sessionId))
        } else {
            SavedStateHandle()
        }
        return HistoryDetailViewModel(
            savedStateHandle = handle,
            workoutHistoryRepository = historyRepository
        )
    }

    @Test
    fun defensiveSessionIdParsing_invalidOrMissingId_producesMissingState() = runTest {
        val vmNull = createViewModel(sessionId = null)
        advanceUntilIdle()
        assertTrue(vmNull.uiState.value.isMissing)
        assertFalse(vmNull.uiState.value.isLoading)

        val vmInvalid = createViewModel(sessionId = "not_a_number")
        advanceUntilIdle()
        assertTrue(vmInvalid.uiState.value.isMissing)

        val vmNegative = createViewModel(sessionId = -5L)
        advanceUntilIdle()
        assertTrue(vmNegative.uiState.value.isMissing)
    }

    @Test
    fun sessionNotFound_producesMissingState() = runTest {
        val vm = createViewModel(sessionId = 999L)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isMissing)
        assertNull(state.session)
    }

    @Test
    fun sessionFound_loadsSessionWithSnapshotAndSets() = runTest {
        historyRepository.sessions.add(sampleSession)
        historyRepository.setLogs.addAll(sampleSets)
        historyRepository.refreshFlow()

        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isMissing)
        assertNull(state.errorMessage)

        val session = state.session
        assertNotNull(session)
        assertEquals("Push Day", session?.workoutNameSnapshot)
        assertEquals(1000L, session?.durationSeconds)
        assertEquals(2, session?.sets?.size)
        assertEquals("Bench Press", session?.sets?.get(0)?.exerciseNameSnapshot)
        assertEquals(10, session?.sets?.get(0)?.reps)
        assertEquals(60.0, session?.sets?.get(0)?.weightKg ?: 0.0, 0.001)
    }

    @Test
    fun observeSession_failure_surfacesErrorMessage() = runTest {
        historyRepository.observeSessionError = DataError.Database(RuntimeException("Disk failure"))

        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText.StringResource(R.string.error_database), state.errorMessage)
        assertNull(state.session)
    }

    @Test
    fun retry_reloadsSession() = runTest {
        historyRepository.observeSessionError = DataError.Database(RuntimeException("Disk failure"))
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), vm.uiState.value.errorMessage)

        // Clear error and add session
        historyRepository.observeSessionError = null
        historyRepository.sessions.add(sampleSession)
        historyRepository.refreshFlow()

        vm.onRetry()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("Push Day", state.session?.workoutNameSnapshot)
    }
}
