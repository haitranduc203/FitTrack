package com.haitranduc.fittrack.domain.usecase

import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.validation.ExerciseListResult
import com.haitranduc.fittrack.domain.validation.NameResult
import com.haitranduc.fittrack.testing.FakeTimeProvider
import com.haitranduc.fittrack.testing.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveWorkoutUseCaseTest {

    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveWorkoutUseCase: SaveWorkoutUseCase

    private val sampleExercise1 = Exercise(
        id = "e1",
        name = "Bench Press",
        bodyPart = "chest",
        equipment = "barbell",
        target = "pectorals",
        muscleGroup = "chest",
        secondaryMuscles = emptyList(),
        instructions = emptyList()
    )

    private val sampleExercise2 = Exercise(
        id = "e2",
        name = "Squat",
        bodyPart = "legs",
        equipment = "barbell",
        target = "quads",
        muscleGroup = "legs",
        secondaryMuscles = emptyList(),
        instructions = emptyList()
    )

    @Before
    fun setUp() {
        workoutRepository = FakeWorkoutRepository()
        timeProvider = FakeTimeProvider(currentTime = 5000L)
        saveWorkoutUseCase = SaveWorkoutUseCase(workoutRepository, timeProvider)
    }

    @Test
    fun blankName_returnsInvalidName_doesNotCallRepository() = runTest {
        val workout = Workout(
            id = 0L,
            name = "   ",
            createdAt = 0L,
            updatedAt = 0L,
            exercises = listOf(sampleExercise1)
        )

        val result = saveWorkoutUseCase(workout)

        assertTrue(result is SaveWorkoutResult.InvalidName)
        assertEquals(NameResult.Blank, (result as SaveWorkoutResult.InvalidName).reason)
        assertTrue(workoutRepository.savedWorkouts.isEmpty())
    }

    @Test
    fun overlongName_returnsInvalidName_doesNotCallRepository() = runTest {
        val workout = Workout(
            id = 0L,
            name = "A".repeat(51),
            createdAt = 0L,
            updatedAt = 0L,
            exercises = listOf(sampleExercise1)
        )

        val result = saveWorkoutUseCase(workout)

        assertTrue(result is SaveWorkoutResult.InvalidName)
        assertEquals(NameResult.TooLong, (result as SaveWorkoutResult.InvalidName).reason)
        assertTrue(workoutRepository.savedWorkouts.isEmpty())
    }

    @Test
    fun emptyExercises_returnsInvalidExercises_doesNotCallRepository() = runTest {
        val workout = Workout(
            id = 0L,
            name = "Leg Day",
            createdAt = 0L,
            updatedAt = 0L,
            exercises = emptyList()
        )

        val result = saveWorkoutUseCase(workout)

        assertTrue(result is SaveWorkoutResult.InvalidExercises)
        assertEquals(ExerciseListResult.Empty, (result as SaveWorkoutResult.InvalidExercises).reason)
        assertTrue(workoutRepository.savedWorkouts.isEmpty())
    }

    @Test
    fun duplicateExercises_returnsInvalidExercises_doesNotCallRepository() = runTest {
        val workout = Workout(
            id = 0L,
            name = "Leg Day",
            createdAt = 0L,
            updatedAt = 0L,
            exercises = listOf(sampleExercise1, sampleExercise1)
        )

        val result = saveWorkoutUseCase(workout)

        assertTrue(result is SaveWorkoutResult.InvalidExercises)
        val duplicateError = (result as SaveWorkoutResult.InvalidExercises).reason
        assertTrue(duplicateError is ExerciseListResult.Duplicate)
        assertEquals("e1", (duplicateError as ExerciseListResult.Duplicate).exerciseId)
        assertTrue(workoutRepository.savedWorkouts.isEmpty())
    }

    @Test
    fun validCreate_trimsName_setsTimestamps_preservesOrder_returnsId() = runTest {
        timeProvider.currentTime = 12345L
        val workout = Workout(
            id = 0L,
            name = "  Upper Body Blast  ",
            createdAt = 0L,
            updatedAt = 0L,
            exercises = listOf(sampleExercise1, sampleExercise2)
        )

        val result = saveWorkoutUseCase(workout)

        assertTrue(result is SaveWorkoutResult.Success)
        val savedId = (result as SaveWorkoutResult.Success).workoutId
        assertEquals(1L, savedId)

        assertEquals(1, workoutRepository.savedWorkouts.size)
        val saved = workoutRepository.savedWorkouts[0]
        assertEquals("Upper Body Blast", saved.name)
        assertEquals(12345L, saved.createdAt)
        assertEquals(12345L, saved.updatedAt)
        assertEquals(listOf(sampleExercise1, sampleExercise2), saved.exercises)
    }

    @Test
    fun validEdit_preservesCreatedAt_updatesUpdatedAt() = runTest {
        timeProvider.currentTime = 99999L
        val workout = Workout(
            id = 42L,
            name = "Full Body",
            createdAt = 1000L,
            updatedAt = 2000L,
            exercises = listOf(sampleExercise2, sampleExercise1)
        )

        val result = saveWorkoutUseCase(workout)

        assertTrue(result is SaveWorkoutResult.Success)
        assertEquals(42L, (result as SaveWorkoutResult.Success).workoutId)

        val saved = workoutRepository.savedWorkouts[0]
        assertEquals(42L, saved.id)
        assertEquals("Full Body", saved.name)
        assertEquals(1000L, saved.createdAt)
        assertEquals(99999L, saved.updatedAt)
        assertEquals(listOf(sampleExercise2, sampleExercise1), saved.exercises)
    }

    @Test
    fun repositoryFailure_mapsToRepositoryError() = runTest {
        workoutRepository.saveError = DataError.Database(RuntimeException("Disk full"))
        val workout = Workout(
            id = 0L,
            name = "Push Day",
            createdAt = 0L,
            updatedAt = 0L,
            exercises = listOf(sampleExercise1)
        )

        val result = saveWorkoutUseCase(workout)

        assertTrue(result is SaveWorkoutResult.RepositoryError)
        val error = (result as SaveWorkoutResult.RepositoryError).error
        assertTrue(error is DataError.Database)
    }
}
