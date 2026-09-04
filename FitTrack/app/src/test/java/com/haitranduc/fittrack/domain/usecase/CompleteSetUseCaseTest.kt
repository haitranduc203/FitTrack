package com.haitranduc.fittrack.domain.usecase

import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.testing.FakeTimeProvider
import com.haitranduc.fittrack.testing.FakeWorkoutHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CompleteSetUseCaseTest {

    private lateinit var workoutHistoryRepository: FakeWorkoutHistoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var completeSetUseCase: CompleteSetUseCase

    private val activeSession = WorkoutSession(
        id = 10L,
        workoutId = 1L,
        workoutNameSnapshot = "Push Day",
        startedAt = 1000L,
        finishedAt = null,
        durationSeconds = null,
        sets = emptyList()
    )

    @Before
    fun setUp() {
        workoutHistoryRepository = FakeWorkoutHistoryRepository()
        timeProvider = FakeTimeProvider(currentTime = 6000L)
        completeSetUseCase = CompleteSetUseCase(
            workoutHistoryRepository = workoutHistoryRepository,
            timeProvider = timeProvider
        )
    }

    @Test
    fun completeSet_success_persistsSetImmediatelyWithExactSnapshot() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)

        val result = completeSetUseCase(
            sessionId = 10L,
            exerciseId = "e1",
            exerciseName = "Bench Press",
            setNumber = 1,
            reps = 10,
            weightKg = 50.0
        )

        assertTrue(result is CompleteSetResult.Success)
        val setId = (result as CompleteSetResult.Success).setId

        assertEquals(1, workoutHistoryRepository.setLogs.size)
        val savedSet = workoutHistoryRepository.setLogs[0]
        assertEquals(setId, savedSet.id)
        assertEquals(10L, savedSet.sessionId)
        assertEquals("e1", savedSet.exerciseId)
        assertEquals("Bench Press", savedSet.exerciseNameSnapshot)
        assertEquals(1, savedSet.setNumber)
        assertEquals(10, savedSet.reps)
        assertEquals(50.0, savedSet.weightKg, 0.001)
        assertEquals(6000L, savedSet.completedAt)
    }

    @Test
    fun completeSet_boundaryValues_reps1And100_weight0And1000_areValid() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)

        val res1 = completeSetUseCase(10L, "e1", "Bench Press", 1, 1, 0.0)
        assertTrue(res1 is CompleteSetResult.Success)

        val res2 = completeSetUseCase(10L, "e1", "Bench Press", 2, 100, 1000.0)
        assertTrue(res2 is CompleteSetResult.Success)

        assertEquals(2, workoutHistoryRepository.setLogs.size)
    }

    @Test
    fun completeSet_invalidReps_rejectedWithoutWriting() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)

        assertEquals(CompleteSetResult.InvalidReps, completeSetUseCase(10L, "e1", "Bench Press", 1, 0, 50.0))
        assertEquals(CompleteSetResult.InvalidReps, completeSetUseCase(10L, "e1", "Bench Press", 1, 101, 50.0))
        assertEquals(CompleteSetResult.InvalidReps, completeSetUseCase(10L, "e1", "Bench Press", 1, -5, 50.0))

        assertTrue(workoutHistoryRepository.setLogs.isEmpty())
    }

    @Test
    fun completeSet_invalidWeight_rejectedWithoutWriting() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)

        assertEquals(CompleteSetResult.InvalidWeight, completeSetUseCase(10L, "e1", "Bench Press", 1, 10, -0.1))
        assertEquals(CompleteSetResult.InvalidWeight, completeSetUseCase(10L, "e1", "Bench Press", 1, 10, 1000.1))
        assertEquals(CompleteSetResult.InvalidWeight, completeSetUseCase(10L, "e1", "Bench Press", 1, 10, Double.NaN))
        assertEquals(CompleteSetResult.InvalidWeight, completeSetUseCase(10L, "e1", "Bench Press", 1, 10, Double.POSITIVE_INFINITY))

        assertTrue(workoutHistoryRepository.setLogs.isEmpty())
    }

    @Test
    fun completeSet_sessionNotFound_returnsSessionNotFound() = runTest {
        val result = completeSetUseCase(999L, "e1", "Bench Press", 1, 10, 50.0)
        assertEquals(CompleteSetResult.SessionNotFound, result)
        assertTrue(workoutHistoryRepository.setLogs.isEmpty())
    }

    @Test
    fun completeSet_sessionAlreadyFinished_returnsSessionAlreadyFinished() = runTest {
        val finishedSession = activeSession.copy(id = 20L, finishedAt = 5000L, durationSeconds = 4000L)
        workoutHistoryRepository.sessions.add(finishedSession)

        val result = completeSetUseCase(20L, "e1", "Bench Press", 1, 10, 50.0)
        assertEquals(CompleteSetResult.SessionAlreadyFinished, result)
        assertTrue(workoutHistoryRepository.setLogs.isEmpty())
    }

    @Test
    fun completeSet_getSessionError_returnsFailure() = runTest {
        workoutHistoryRepository.getSessionError = DataError.Database(RuntimeException("Session lookup failed"))

        val result = completeSetUseCase(10L, "e1", "Bench Press", 1, 10, 50.0)
        assertTrue(result is CompleteSetResult.Failure)
        assertTrue(workoutHistoryRepository.setLogs.isEmpty())
    }

    @Test
    fun completeSet_insertSetError_returnsFailure() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)
        workoutHistoryRepository.insertSetError = DataError.Database(RuntimeException("Insert set failed"))

        val result = completeSetUseCase(10L, "e1", "Bench Press", 1, 10, 50.0)
        assertTrue(result is CompleteSetResult.Failure)
        assertTrue(workoutHistoryRepository.setLogs.isEmpty())
    }

    @Test
    fun completeSet_cancellation_rethrowsCancellationException() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)
        val cancelRepo = object : FakeWorkoutHistoryRepository() {
            override suspend fun insertSet(setLog: SetLog): DataResult<Long> {
                throw CancellationException("Cancelled insert set")
            }
        }
        cancelRepo.sessions.add(activeSession)
        val useCase = CompleteSetUseCase(cancelRepo, timeProvider)

        try {
            useCase(10L, "e1", "Bench Press", 1, 10, 50.0)
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Cancelled insert set", e.message)
        }
    }
}
