package com.haitranduc.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface StatisticsDao {
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE finishedAt IS NOT NULL")
    fun observeTotalWorkouts(): Flow<Long>

    @Query("SELECT COUNT(*) FROM set_logs INNER JOIN workout_sessions ON workout_sessions.id = set_logs.sessionId WHERE workout_sessions.finishedAt IS NOT NULL")
    fun observeTotalCompletedSets(): Flow<Long>
}
