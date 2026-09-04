package com.haitranduc.fittrack.domain.repository

import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface WorkoutHistoryRepository {
    fun observeHistory(): Flow<DataResult<List<WorkoutSession>>>
    fun observeSession(id: Long): Flow<DataResult<WorkoutSession?>>
    suspend fun getActiveSession(): DataResult<WorkoutSession?>
    suspend fun getSession(id: Long): DataResult<WorkoutSession?>
    suspend fun insertSession(session: WorkoutSession): DataResult<Long>
    suspend fun updateSession(session: WorkoutSession): DataResult<Unit>
    suspend fun insertSet(setLog: SetLog): DataResult<Long>
    suspend fun deleteSession(sessionId: Long): DataResult<Unit>
}
