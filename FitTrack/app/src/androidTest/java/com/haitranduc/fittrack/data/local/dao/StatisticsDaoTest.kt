package com.haitranduc.fittrack.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
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

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FitTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        statisticsDao = database.statisticsDao()
        workoutSessionDao = database.workoutSessionDao()
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
}
