package com.haitranduc.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface SetLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSetLog(setLog: SetLogEntity): Long

    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteSetLog(id: Long): Int

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId ASC, setNumber ASC, id ASC")
    fun observeSetLogsForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId ASC, setNumber ASC, id ASC")
    suspend fun getSetLogsForSession(sessionId: Long): List<SetLogEntity>
}
