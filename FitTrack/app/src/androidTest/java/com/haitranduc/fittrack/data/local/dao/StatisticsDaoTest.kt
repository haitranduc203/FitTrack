package com.haitranduc.fittrack.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsDaoTest {

    private lateinit var database: FitTrackDatabase
    private lateinit var statisticsDao: StatisticsDao
    private lateinit var workoutSessionDao: WorkoutSessionDao
    private lateinit var setLogDao: SetLogDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FitTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        statisticsDao = database.statisticsDao()
        workoutSessionDao = database.workoutSessionDao()
        setLogDao = database.setLogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeTotalWorkouts_empty_returnsZero() = runBlocking {
        val total = statisticsDao.observeTotalWorkouts().first()
        assertEquals(0L, total)
    }

    @Test
    fun observeTotalWorkouts_countsOnlyFinishedSessions() = runBlocking {
        val unfinishedSession = WorkoutSessionEntity(
            id = 1L,
            workoutId = null,
            workoutNameSnapshot = "Unfinished Workout",
            startedAt = 1000L,
            finishedAt = null,
            durationSeconds = null
        )
        workoutSessionDao.insertSession(unfinishedSession)

        val totalAfterUnfinished = statisticsDao.observeTotalWorkouts().first()
        assertEquals(0L, totalAfterUnfinished)

        val finishedSession1 = WorkoutSessionEntity(
            id = 2L,
            workoutId = null,
            workoutNameSnapshot = "Finished Workout 1",
            startedAt = 2000L,
            finishedAt = 3000L,
            durationSeconds = 1000L
        )
        workoutSessionDao.insertSession(finishedSession1)

        val totalAfterFinished1 = statisticsDao.observeTotalWorkouts().first()
        assertEquals(1L, totalAfterFinished1)

        val finishedSession2 = WorkoutSessionEntity(
            id = 3L,
            workoutId = null,
            workoutNameSnapshot = "Finished Workout 2",
            startedAt = 4000L,
            finishedAt = 5000L,
            durationSeconds = 1000L
        )
        workoutSessionDao.insertSession(finishedSession2)

        val totalAfterFinished2 = statisticsDao.observeTotalWorkouts().first()
        assertEquals(2L, totalAfterFinished2)
    }

    @Test
    fun observeTotalCompletedSets_empty_returnsZero() = runBlocking {
        val total = statisticsDao.observeTotalCompletedSets().first()
        assertEquals(0L, total)
    }

    @Test
    fun observeTotalCompletedSets_countsOnlySetsFromFinishedSessions() = runBlocking {
        val unfinishedSession = WorkoutSessionEntity(
            id = 1L,
            workoutId = null,
            workoutNameSnapshot = "Unfinished Workout",
            startedAt = 1000L,
            finishedAt = null,
            durationSeconds = null
        )
        workoutSessionDao.insertSession(unfinishedSession)

        // Add 2 sets to unfinished session
        setLogDao.insertSetLog(
            SetLogEntity(
                id = 101L,
                sessionId = 1L,
                exerciseId = "ex1",
                exerciseNameSnapshot = "Bench Press",
                setNumber = 1,
                reps = 10,
                weightKg = 60.0,
                completedAt = 1100L
            )
        )
        setLogDao.insertSetLog(
            SetLogEntity(
                id = 102L,
                sessionId = 1L,
                exerciseId = "ex1",
                exerciseNameSnapshot = "Bench Press",
                setNumber = 2,
                reps = 8,
                weightKg = 65.0,
                completedAt = 1200L
            )
        )

        val totalAfterUnfinished = statisticsDao.observeTotalCompletedSets().first()
        assertEquals(0L, totalAfterUnfinished)

        val finishedSession = WorkoutSessionEntity(
            id = 2L,
            workoutId = null,
            workoutNameSnapshot = "Finished Workout",
            startedAt = 2000L,
            finishedAt = 3000L,
            durationSeconds = 1000L
        )
        workoutSessionDao.insertSession(finishedSession)

        // Add 3 sets to finished session
        setLogDao.insertSetLog(
            SetLogEntity(
                id = 201L,
                sessionId = 2L,
                exerciseId = "ex1",
                exerciseNameSnapshot = "Bench Press",
                setNumber = 1,
                reps = 10,
                weightKg = 60.0,
                completedAt = 2100L
            )
        )
        setLogDao.insertSetLog(
            SetLogEntity(
                id = 202L,
                sessionId = 2L,
                exerciseId = "ex1",
                exerciseNameSnapshot = "Bench Press",
                setNumber = 2,
                reps = 8,
                weightKg = 65.0,
                completedAt = 2200L
            )
        )
        setLogDao.insertSetLog(
            SetLogEntity(
                id = 203L,
                sessionId = 2L,
                exerciseId = "ex2",
                exerciseNameSnapshot = "Incline Dumbbell Press",
                setNumber = 1,
                reps = 12,
                weightKg = 22.0,
                completedAt = 2300L
            )
        )

        val totalAfterFinished = statisticsDao.observeTotalCompletedSets().first()
        assertEquals(3L, totalAfterFinished)
    }
}
