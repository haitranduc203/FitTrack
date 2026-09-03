package com.haitranduc.fittrack.data.local.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var database: FitTrackDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao

    private val exerciseA = ExerciseEntity(
        id = "ex_A",
        name = "Bench Press",
        bodyPart = "Chest",
        equipment = "Barbell",
        target = "Pecs",
        muscleGroup = "Chest",
        secondaryMuscles = emptyList(),
        instructions = listOf("Push")
    )

    private val exerciseB = ExerciseEntity(
        id = "ex_B",
        name = "Incline Dumbbell Press",
        bodyPart = "Chest",
        equipment = "Dumbbell",
        target = "Upper Pecs",
        muscleGroup = "Chest",
        secondaryMuscles = emptyList(),
        instructions = listOf("Press")
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FitTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = database.workoutDao()
        exerciseDao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveWorkout_createsNewWorkoutWithOrderedExercises() = runTest {
        exerciseDao.insertAll(listOf(exerciseA, exerciseB))

        val workout = WorkoutEntity(name = "Push Day", createdAt = 1000L, updatedAt = 1000L)
        val workoutId = workoutDao.saveWorkout(workout, listOf("ex_A", "ex_B"))
        assertTrue(workoutId > 0)

        val workoutWithExercises = workoutDao.observeWorkoutWithExercises(workoutId).first()
        assertNotNull(workoutWithExercises)
        assertEquals("Push Day", workoutWithExercises?.workout?.name)
        assertEquals(2, workoutWithExercises?.exercises?.size)
        assertEquals("ex_A", workoutWithExercises?.exercises?.get(0)?.exercise?.id)
        assertEquals(0, workoutWithExercises?.exercises?.get(0)?.orderIndex)
        assertEquals("ex_B", workoutWithExercises?.exercises?.get(1)?.exercise?.id)
        assertEquals(1, workoutWithExercises?.exercises?.get(1)?.orderIndex)
    }

    @Test
    fun observeWorkouts_ordersByMostRecentlyUpdated() = runTest {
        val w1 = WorkoutEntity(name = "Old Workout", createdAt = 100L, updatedAt = 200L)
        val w2 = WorkoutEntity(name = "New Workout", createdAt = 300L, updatedAt = 400L)

        val id1 = workoutDao.insertWorkout(w1)
        val id2 = workoutDao.insertWorkout(w2)

        val workouts = workoutDao.observeWorkouts().first()
        assertEquals(2, workouts.size)
        assertEquals(id2, workouts[0].id)
        assertEquals(id1, workouts[1].id)
    }

    @Test
    fun saveWorkout_updatesExistingWorkoutAndReordersExercises() = runTest {
        exerciseDao.insertAll(listOf(exerciseA, exerciseB))

        val workout = WorkoutEntity(name = "Initial", createdAt = 1000L, updatedAt = 1000L)
        val workoutId = workoutDao.saveWorkout(workout, listOf("ex_A", "ex_B"))

        val updated = WorkoutEntity(id = workoutId, name = "Updated Push", createdAt = 1000L, updatedAt = 2000L)
        val returnedId = workoutDao.saveWorkout(updated, listOf("ex_B", "ex_A"))
        assertEquals(workoutId, returnedId)

        val result = workoutDao.observeWorkoutWithExercises(workoutId).first()
        assertEquals("Updated Push", result?.workout?.name)
        assertEquals("ex_B", result?.exercises?.get(0)?.exercise?.id)
        assertEquals(0, result?.exercises?.get(0)?.orderIndex)
        assertEquals("ex_A", result?.exercises?.get(1)?.exercise?.id)
        assertEquals(1, result?.exercises?.get(1)?.orderIndex)
    }

    @Test
    fun duplicateExercise_failsWithConstraintException() = runTest {
        exerciseDao.insertAll(listOf(exerciseA))
        val workout = WorkoutEntity(name = "Duplicate Ex", createdAt = 1000L, updatedAt = 1000L)
        val workoutId = workoutDao.insertWorkout(workout)

        workoutDao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(workoutId = workoutId, exerciseId = "ex_A", orderIndex = 0)
            )
        )

        try {
            workoutDao.insertWorkoutExercises(
                listOf(
                    WorkoutExerciseEntity(workoutId = workoutId, exerciseId = "ex_A", orderIndex = 1)
                )
            )
            fail("Expected SQLiteConstraintException on duplicate exerciseId")
        } catch (e: SQLiteConstraintException) {
            // expected primary key violation
        }
    }

    @Test
    fun duplicateOrderIndex_failsWithConstraintException() = runTest {
        exerciseDao.insertAll(listOf(exerciseA, exerciseB))
        val workout = WorkoutEntity(name = "Duplicate Order", createdAt = 1000L, updatedAt = 1000L)
        val workoutId = workoutDao.insertWorkout(workout)

        try {
            workoutDao.insertWorkoutExercises(
                listOf(
                    WorkoutExerciseEntity(workoutId = workoutId, exerciseId = "ex_A", orderIndex = 0),
                    WorkoutExerciseEntity(workoutId = workoutId, exerciseId = "ex_B", orderIndex = 0)
                )
            )
            fail("Expected SQLiteConstraintException on duplicate orderIndex")
        } catch (e: SQLiteConstraintException) {
            // expected unique constraint on (workoutId, orderIndex)
        }
    }

    @Test
    fun unknownExerciseForeignKey_failsWithConstraintException() = runTest {
        val workout = WorkoutEntity(name = "Unknown Ex", createdAt = 1000L, updatedAt = 1000L)
        val workoutId = workoutDao.insertWorkout(workout)

        try {
            workoutDao.insertWorkoutExercises(
                listOf(
                    WorkoutExerciseEntity(workoutId = workoutId, exerciseId = "non_existent", orderIndex = 0)
                )
            )
            fail("Expected SQLiteConstraintException on unknown exerciseId foreign key")
        } catch (e: SQLiteConstraintException) {
            // expected foreign key constraint violation
        }
    }

    @Test
    fun deleteWorkout_cascadesToJunctionRows() = runTest {
        exerciseDao.insertAll(listOf(exerciseA))
        val workout = WorkoutEntity(name = "To Delete", createdAt = 1000L, updatedAt = 1000L)
        val workoutId = workoutDao.saveWorkout(workout, listOf("ex_A"))

        assertEquals(1, workoutDao.observeWorkoutExercises(workoutId).first().size)

        workoutDao.deleteWorkout(workoutId)

        assertNull(workoutDao.observeWorkoutById(workoutId).first())
        assertEquals(0, workoutDao.observeWorkoutExercises(workoutId).first().size)
    }

    @Test
    fun deleteEmptyWorkout_succeeds() = runTest {
        val workout = WorkoutEntity(name = "Empty Draft", createdAt = 1000L, updatedAt = 1000L)
        val workoutId = workoutDao.insertWorkout(workout)

        val count = workoutDao.deleteWorkout(workoutId)
        assertEquals(1, count)
        assertNull(workoutDao.observeWorkoutById(workoutId).first())
    }

    @Test
    fun rollbackWhenReplacementRowFails() = runTest {
        exerciseDao.insertAll(listOf(exerciseA))
        val workout = WorkoutEntity(name = "Rollback Test", createdAt = 1000L, updatedAt = 1000L)
        val workoutId = workoutDao.saveWorkout(workout, listOf("ex_A"))

        try {
            // Try saving with an invalid exercise foreign key
            workoutDao.saveWorkout(
                WorkoutEntity(id = workoutId, name = "Rollback Test", createdAt = 1000L, updatedAt = 2000L),
                listOf("ex_A", "non_existent_exercise")
            )
            fail("Expected constraint failure")
        } catch (e: SQLiteConstraintException) {
            // Expected
        }

        // Previous junction rows should still exist because transaction rolled back
        val exercises = workoutDao.observeWorkoutExercises(workoutId).first()
        assertEquals(1, exercises.size)
        assertEquals("ex_A", exercises[0].exercise.id)
    }
}
