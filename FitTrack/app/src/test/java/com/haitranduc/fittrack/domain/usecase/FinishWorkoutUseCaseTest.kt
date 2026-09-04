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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class FinishWorkoutUseCaseTest {

    private lateinit var workoutHistoryRepository: FakeWorkoutHistoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var finishWorkoutUseCase: FinishWorkoutUseCase

    private val activeSession = WorkoutSession(
        id = 10L,
        workoutId = 1L,
        workoutNameSnapshot = "Push Day",
        startedAt = 1000L,
        finishedAt = null,
        durationSeconds = null,
        sets = emptyList()
    )

    private val sampleSet = SetLog(
        id = 1L,
        sessionId = 10L,
        exerciseId = "e1",
        exerciseNameSnapshot = "Bench Press",
        setNumber = 1,
        reps = 10,
        weightKg = 50.0,
        completedAt = 2000L
    )

    @Before
    fun setUp() {
        workoutHistoryRepository = FakeWorkoutHistoryRepository()
        timeProvider = FakeTimeProvider(currentTime = 61_500L) // 61.5s after startedAt 1000L -> duration = (61500 - 1000) / 1000 = 60s
        finishWorkoutUseCase = FinishWorkoutUseCase(
            workoutHistoryRepository = workoutHistoryRepository,
            timeProvider = timeProvider
        )
    }

    @Test
    fun finishWorkout_success_updatesSessionWithExactFinishedAtAndFloorDuration() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)
        workoutHistoryRepository.setLogs.add(sampleSet)

        val result = finishWorkoutUseCase(10L)
        assertTrue(result is FinishWorkoutResult.Success)
        assertEquals(10L, (result as FinishWorkoutResult.Success).sessionId)

        val updatedSession = workoutHistoryRepository.sessions.find { it.id == 10L }
        assertNotNull(updatedSession)
        assertEquals(61_500L, updatedSession?.finishedAt)
        // 61500 - 1000 = 60500 ms -> 60 seconds
        assertEquals(60L, updatedSession?.durationSeconds)
    }

    @Test
    fun finishWorkout_noCompletedSets_rejectedWithNoSetsCompleted() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)
        // Zero sets

        val result = finishWorkoutUseCase(10L)
        assertEquals(FinishWorkoutResult.NoSetsCompleted, result)

        val session = workoutHistoryRepository.sessions.find { it.id == 10L }
        assertEquals(null, session?.finishedAt)
    }

    @Test
    fun finishWorkout_sessionNotFound_returnsSessionNotFound() = runTest {
        val result = finishWorkoutUseCase(999L)
        assertEquals(FinishWorkoutResult.SessionNotFound, result)
    }

    @Test
    fun finishWorkout_sessionAlreadyFinished_returnsSessionAlreadyFinished() = runTest {
        val alreadyFinishedSession = activeSession.copy(
            id = 20L,
            finishedAt = 50_000L,
            durationSeconds = 49L
        )
        workoutHistoryRepository.sessions.add(alreadyFinishedSession)
        workoutHistoryRepository.setLogs.add(sampleSet.copy(sessionId = 20L))

        val result = finishWorkoutUseCase(20L)
        assertEquals(FinishWorkoutResult.SessionAlreadyFinished, result)
    }

    @Test
    fun finishWorkout_secondFinish_prevented() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)
        workoutHistoryRepository.setLogs.add(sampleSet)

        val firstFinish = finishWorkoutUseCase(10L)
        assertTrue(firstFinish is FinishWorkoutResult.Success)

        val secondFinish = finishWorkoutUseCase(10L)
        assertEquals(FinishWorkoutResult.SessionAlreadyFinished, secondFinish)
    }

    @Test
    fun finishWorkout_getSessionFails_returnsFailure() = runTest {
        workoutHistoryRepository.getSessionError = DataError.Database(RuntimeException("DB error"))

        val result = finishWorkoutUseCase(10L)
        assertTrue(result is FinishWorkoutResult.Failure)
    }

    @Test
    fun finishWorkout_updateSessionFails_returnsFailure() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)
        workoutHistoryRepository.setLogs.add(sampleSet)
        workoutHistoryRepository.updateSessionError = DataError.Database(RuntimeException("Update failed"))

        val result = finishWorkoutUseCase(10L)
        assertTrue(result is FinishWorkoutResult.Failure)
    }

    @Test
    fun finishWorkout_negativeDuration_clampedToZero() = runTest {
        // If system clock went backwards
        timeProvider.currentTime = 500L // before startedAt = 1000L
        workoutHistoryRepository.sessions.add(activeSession)
        workoutHistoryRepository.setLogs.add(sampleSet)

        val result = finishWorkoutUseCase(10L)
        assertTrue(result is FinishWorkoutResult.Success)

        val updatedSession = workoutHistoryRepository.sessions.find { it.id == 10L }
        assertEquals(0L, updatedSession?.durationSeconds)
    }

    @Test
    fun finishWorkout_cancellation_rethrowsCancellationException() = runTest {
        workoutHistoryRepository.sessions.add(activeSession)
        workoutHistoryRepository.setLogs.add(sampleSet)

        val cancelRepo = object : FakeWorkoutHistoryRepository() {
            override suspend fun updateSession(session: WorkoutSession): DataResult<Unit> {
                throw CancellationException("Update cancelled")
            }
        }
        cancelRepo.sessions.add(activeSession)
        cancelRepo.setLogs.add(sampleSet)
        val useCase = FinishWorkoutUseCase(cancelRepo, timeProvider)

        try {
            useCase(10L)
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Update cancelled", e.message)
        }
    }
}
