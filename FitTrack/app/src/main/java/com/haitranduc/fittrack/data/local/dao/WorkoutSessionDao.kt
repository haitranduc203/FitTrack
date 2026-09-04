package com.haitranduc.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity
import com.haitranduc.fittrack.data.local.relation.WorkoutSessionWithSets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Dao
@JvmSuppressWildcards
interface WorkoutSessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity): Int

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long): Int

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSessionById(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC, id DESC")
    fun observeFinishedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId ASC, setNumber ASC, id ASC")
    fun observeSetsForSession(sessionId: Long): Flow<List<SetLogEntity>>

    fun observeSessionWithSets(sessionId: Long): Flow<WorkoutSessionWithSets?> {
        return combine(observeSessionById(sessionId), observeSetsForSession(sessionId)) { session, sets ->
            session?.let { WorkoutSessionWithSets(it, sets) }
        }
    }
}
