package com.haitranduc.fittrack.data.local.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity
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
class WorkoutHistoryDaoTest {

    private lateinit var database: FitTrackDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var workoutSessionDao: WorkoutSessionDao
    private lateinit var setLogDao: SetLogDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FitTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = database.workoutDao()
        workoutSessionDao = database.workoutSessionDao()
        setLogDao = database.setLogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun activeSession_lookupReturnsUnfinishedSessionOnly() = runTest {
        val session1 = WorkoutSessionEntity(
            workoutId = null,
            workoutNameSnapshot = "Quick Workout",
            startedAt = 1000L,
            finishedAt = null,
            durationSeconds = null
        )
        val id1 = workoutSessionDao.insertSession(session1)

        val active = workoutSessionDao.getActiveSession()
        assertNotNull(active)
        assertEquals(id1, active?.id)

        // Finish session1
        workoutSessionDao.updateSession(active!!.copy(finishedAt = 2000L, durationSeconds = 1000L))
        val activeAfterFinish = workoutSessionDao.getActiveSession()
        assertNull(activeAfterFinish)
    }

    @Test
    fun finishedSessions_observeFinishedOnly_newestFirst() = runTest {
        val s1 = WorkoutSessionEntity(workoutId = null, workoutNameSnapshot = "S1", startedAt = 100L, finishedAt = 500L, durationSeconds = 400L)
        val s2 = WorkoutSessionEntity(workoutId = null, workoutNameSnapshot = "S2", startedAt = 600L, finishedAt = 1200L, durationSeconds = 600L)
        val sUnfinished = WorkoutSessionEntity(workoutId = null, workoutNameSnapshot = "Active", startedAt = 1300L, finishedAt = null, durationSeconds = null)

        val id1 = workoutSessionDao.insertSession(s1)
        val id2 = workoutSessionDao.insertSession(s2)
        workoutSessionDao.insertSession(sUnfinished)

        val finished = workoutSessionDao.observeFinishedSessions().first()
        assertEquals(2, finished.size)
        assertEquals(id2, finished[0].id)
        assertEquals(id1, finished[1].id)
    }

    @Test
    fun sessionWithSets_ordersSetsDeterministically() = runTest {
        val sessionId = workoutSessionDao.insertSession(
            WorkoutSessionEntity(workoutId = null, workoutNameSnapshot = "Leg Day", startedAt = 1000L, finishedAt = 2000L, durationSeconds = 1000L)
        )

        // Insert sets out of order
        val setB2 = SetLogEntity(sessionId = sessionId, exerciseId = "ex_B", exerciseNameSnapshot = "Squat", setNumber = 2, reps = 8, weightKg = 100.0, completedAt = 1500L)
        val setA1 = SetLogEntity(sessionId = sessionId, exerciseId = "ex_A", exerciseNameSnapshot = "Leg Press", setNumber = 1, reps = 12, weightKg = 200.0, completedAt = 1200L)
        val setB1 = SetLogEntity(sessionId = sessionId, exerciseId = "ex_B", exerciseNameSnapshot = "Squat", setNumber = 1, reps = 10, weightKg = 90.0, completedAt = 1400L)

        setLogDao.insertSetLog(setB2)
        setLogDao.insertSetLog(setA1)
        setLogDao.insertSetLog(setB1)

        val sessionWithSets = workoutSessionDao.observeSessionWithSets(sessionId).first()
        assertNotNull(sessionWithSets)
        assertEquals(3, sessionWithSets?.sets?.size)
        // Ordered by exerciseId ("ex_A" then "ex_B"), then setNumber (1, then 2)
        assertEquals("ex_A", sessionWithSets?.sets?.get(0)?.exerciseId)
        assertEquals(1, sessionWithSets?.sets?.get(0)?.setNumber)

        assertEquals("ex_B", sessionWithSets?.sets?.get(1)?.exerciseId)
        assertEquals(1, sessionWithSets?.sets?.get(1)?.setNumber)

        assertEquals("ex_B", sessionWithSets?.sets?.get(2)?.exerciseId)
        assertEquals(2, sessionWithSets?.sets?.get(2)?.setNumber)
    }

    @Test
    fun duplicateSet_rejectedByConstraint() = runTest {
        val sessionId = workoutSessionDao.insertSession(
            WorkoutSessionEntity(workoutId = null, workoutNameSnapshot = "Session", startedAt = 1000L, finishedAt = null, durationSeconds = null)
        )

        val set1 = SetLogEntity(sessionId = sessionId, exerciseId = "ex_A", exerciseNameSnapshot = "Bench", setNumber = 1, reps = 10, weightKg = 80.0, completedAt = 1100L)
        setLogDao.insertSetLog(set1)

        try {
            val duplicateSet = SetLogEntity(sessionId = sessionId, exerciseId = "ex_A", exerciseNameSnapshot = "Bench", setNumber = 1, reps = 12, weightKg = 85.0, completedAt = 1200L)
            setLogDao.insertSetLog(duplicateSet)
            fail("Expected SQLiteConstraintException for duplicate (sessionId, exerciseId, setNumber)")
        } catch (e: SQLiteConstraintException) {
            // Expected
        }
    }

    @Test
    fun deleteSetLog_removesIndividualSet() = runTest {
        val sessionId = workoutSessionDao.insertSession(
            WorkoutSessionEntity(workoutId = null, workoutNameSnapshot = "Session", startedAt = 1000L, finishedAt = null, durationSeconds = null)
        )

        val set1Id = setLogDao.insertSetLog(
            SetLogEntity(sessionId = sessionId, exerciseId = "ex_A", exerciseNameSnapshot = "Bench", setNumber = 1, reps = 10, weightKg = 80.0, completedAt = 1100L)
        )

        val setsBefore = setLogDao.observeSetLogsForSession(sessionId).first()
        assertEquals(1, setsBefore.size)

        setLogDao.deleteSetLog(set1Id)

        val setsAfter = setLogDao.observeSetLogsForSession(sessionId).first()
        assertEquals(0, setsAfter.size)
    }

    @Test
    fun deleteSession_cascadesToSets() = runTest {
        val sessionId = workoutSessionDao.insertSession(
            WorkoutSessionEntity(workoutId = null, workoutNameSnapshot = "Session", startedAt = 1000L, finishedAt = 2000L, durationSeconds = 1000L)
        )

        setLogDao.insertSetLog(
            SetLogEntity(sessionId = sessionId, exerciseId = "ex_A", exerciseNameSnapshot = "Bench", setNumber = 1, reps = 10, weightKg = 80.0, completedAt = 1100L)
        )

        workoutSessionDao.deleteSession(sessionId)

        assertNull(workoutSessionDao.observeSessionById(sessionId).first())
        val sets = setLogDao.observeSetLogsForSession(sessionId).first()
        assertEquals(0, sets.size)
    }

    @Test
    fun deletingWorkoutTemplate_preservesCompletedHistoryAndSetsSnapshots() = runTest {
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(name = "Push Day", createdAt = 1000L, updatedAt = 1000L)
        )

        val sessionId = workoutSessionDao.insertSession(
            WorkoutSessionEntity(
                workoutId = workoutId,
                workoutNameSnapshot = "Push Day Snapshot",
                startedAt = 1500L,
                finishedAt = 2500L,
                durationSeconds = 1000L
            )
        )

        setLogDao.insertSetLog(
            SetLogEntity(
                sessionId = sessionId,
                exerciseId = "ex_Bench",
                exerciseNameSnapshot = "Barbell Bench Press Snapshot",
                setNumber = 1,
                reps = 10,
                weightKg = 80.0,
                completedAt = 1800L
            )
        )

        // Now delete the template
        workoutDao.deleteWorkout(workoutId)

        // Workout template is deleted
        assertNull(workoutDao.observeWorkoutById(workoutId).first())

        // Completed session MUST still exist!
        val sessionWithSets = workoutSessionDao.observeSessionWithSets(sessionId).first()
        assertNotNull(sessionWithSets)
        assertEquals("Push Day Snapshot", sessionWithSets?.session?.workoutNameSnapshot)
        // Foreign key was set to null (ON DELETE SET NULL)
        assertNull(sessionWithSets?.session?.workoutId)
        assertEquals(2500L, sessionWithSets?.session?.finishedAt)

        // Set log snapshot MUST still exist!
        assertEquals(1, sessionWithSets?.sets?.size)
        assertEquals("Barbell Bench Press Snapshot", sessionWithSets?.sets?.get(0)?.exerciseNameSnapshot)
    }
}
