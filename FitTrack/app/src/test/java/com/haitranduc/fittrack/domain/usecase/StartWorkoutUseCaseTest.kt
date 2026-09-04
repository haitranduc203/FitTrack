package com.haitranduc.fittrack.domain.usecase

import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.testing.FakeTimeProvider
import com.haitranduc.fittrack.testing.FakeWorkoutHistoryRepository
import com.haitranduc.fittrack.testing.FakeWorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class StartWorkoutUseCaseTest {

    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var workoutHistoryRepository: FakeWorkoutHistoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var startWorkoutUseCase: StartWorkoutUseCase

    private val sampleExercise = Exercise(
        id = "e1",
        name = "Bench Press",
        bodyPart = "chest",
        equipment = "barbell",
        target = "pectorals",
        muscleGroup = "chest",
        secondaryMuscles = emptyList(),
        instructions = emptyList()
    )

    private val sampleWorkout = Workout(
        id = 1L,
        name = "Push Day",
        createdAt = 1000L,
        updatedAt = 1000L,
        exercises = listOf(sampleExercise)
    )

    @Before
    fun setUp() {
        workoutRepository = FakeWorkoutRepository()
        workoutHistoryRepository = FakeWorkoutHistoryRepository()
        timeProvider = FakeTimeProvider(currentTime = 5000L)
        startWorkoutUseCase = StartWorkoutUseCase(
            workoutRepository = workoutRepository,
            workoutHistoryRepository = workoutHistoryRepository,
            timeProvider = timeProvider
        )
    }

    @Test
    fun startWorkout_success_createsSessionWithExactSnapshotAndTimestamp() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout))

        val result = startWorkoutUseCase(1L)
        assertTrue(result is StartWorkoutResult.Success)
        val sessionId = (result as StartWorkoutResult.Success).sessionId

        assertEquals(1, workoutHistoryRepository.sessions.size)
        val session = workoutHistoryRepository.sessions[0]
        assertEquals(sessionId, session.id)
        assertEquals(1L, session.workoutId)
        assertEquals("Push Day", session.workoutNameSnapshot)
        assertEquals(5000L, session.startedAt)
        assertNull(session.finishedAt)
        assertNull(session.durationSeconds)
        assertTrue(session.sets.isEmpty())
    }

    @Test
    fun startWorkout_workoutNotFound_returnsWorkoutNotFound() = runTest {
        workoutRepository.setWorkouts(emptyList())

        val result = startWorkoutUseCase(999L)
        assertEquals(StartWorkoutResult.WorkoutNotFound, result)
        assertTrue(workoutHistoryRepository.sessions.isEmpty())
    }

    @Test
    fun startWorkout_workoutWithZeroExercises_returnsEmptyWorkout() = runTest {
        val emptyWorkout = sampleWorkout.copy(id = 2L, exercises = emptyList())
        workoutRepository.setWorkouts(listOf(emptyWorkout))

        val result = startWorkoutUseCase(2L)
        assertEquals(StartWorkoutResult.EmptyWorkout, result)
        assertTrue(workoutHistoryRepository.sessions.isEmpty())
    }

    @Test
    fun startWorkout_activeSessionAlreadyExists_returnsActiveSessionExistsWithId() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout))
        val existingActiveSession = WorkoutSession(
            id = 42L,
            workoutId = 1L,
            workoutNameSnapshot = "Push Day",
            startedAt = 2000L,
            finishedAt = null,
            durationSeconds = null,
            sets = emptyList()
        )
        workoutHistoryRepository.sessions.add(existingActiveSession)

        val result = startWorkoutUseCase(1L)
        assertTrue(result is StartWorkoutResult.ActiveSessionExists)
        assertEquals(42L, (result as StartWorkoutResult.ActiveSessionExists).sessionId)
        // No new session created
        assertEquals(1, workoutHistoryRepository.sessions.size)
    }

    @Test
    fun startWorkout_observeWorkoutFails_returnsFailure() = runTest {
        workoutRepository.observeError = DataError.Database(RuntimeException("Disk error"))

        val result = startWorkoutUseCase(1L)
        assertTrue(result is StartWorkoutResult.Failure)
        assertTrue(workoutHistoryRepository.sessions.isEmpty())
    }

    @Test
    fun startWorkout_getActiveSessionFails_returnsFailure() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout))
        workoutHistoryRepository.getActiveSessionError = DataError.Database(RuntimeException("DB query error"))

        val result = startWorkoutUseCase(1L)
        assertTrue(result is StartWorkoutResult.Failure)
        assertTrue(workoutHistoryRepository.sessions.isEmpty())
    }

    @Test
    fun startWorkout_insertSessionFails_returnsFailure() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout))
        workoutHistoryRepository.insertSessionError = DataError.Database(RuntimeException("Insert failed"))

        val result = startWorkoutUseCase(1L)
        assertTrue(result is StartWorkoutResult.Failure)
        assertTrue(workoutHistoryRepository.sessions.isEmpty())
    }

    @Test
    fun startWorkout_cancellation_rethrowsCancellationException() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout))
        workoutHistoryRepository.insertSessionError = null

        // If a cancellation exception is thrown, it must not be swallowed into Failure
        val cancelRepo = object : FakeWorkoutHistoryRepository() {
            override suspend fun insertSession(session: WorkoutSession): com.haitranduc.fittrack.domain.repository.DataResult<Long> {
                throw CancellationException("Cancelled")
            }
        }
        val useCase = StartWorkoutUseCase(workoutRepository, cancelRepo, timeProvider)

        try {
            useCase(1L)
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Cancelled", e.message)
        }
    }

    @Test
    fun startWorkout_concurrentStarts_resultsInExactlyOneInsert() = runTest {
        workoutRepository.setWorkouts(listOf(sampleWorkout))

        val deferred1 = async { startWorkoutUseCase(1L) }
        val deferred2 = async { startWorkoutUseCase(1L) }

        val results = awaitAll(deferred1, deferred2)
        assertEquals(1, workoutHistoryRepository.sessions.size)
        val successCount = results.count { it is StartWorkoutResult.Success }
        val activeExistsCount = results.count { it is StartWorkoutResult.ActiveSessionExists }
        assertEquals(1, successCount)
        assertEquals(1, activeExistsCount)
    }
}
