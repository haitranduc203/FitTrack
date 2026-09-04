package com.haitranduc.fittrack.data.repository

import android.database.sqlite.SQLiteException
import com.haitranduc.fittrack.data.local.dao.SetLogDao
import com.haitranduc.fittrack.data.local.dao.WorkoutSessionDao
import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity
import com.haitranduc.fittrack.data.local.relation.WorkoutSessionWithSets
import com.haitranduc.fittrack.data.mapper.toDomain
import com.haitranduc.fittrack.data.mapper.toEntity
import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutHistoryRepositoryImpl @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val setLogDao: SetLogDao
) : WorkoutHistoryRepository {

    override fun observeHistory(): Flow<DataResult<List<WorkoutSession>>> {
        return workoutSessionDao.observeFinishedSessions()
            .map<List<WorkoutSessionEntity>, DataResult<List<WorkoutSession>>> { sessionEntities ->
                val sessions = sessionEntities.map { entity ->
                    WorkoutSession(
                        id = entity.id,
                        workoutId = entity.workoutId,
                        workoutNameSnapshot = entity.workoutNameSnapshot,
                        startedAt = entity.startedAt,
                        finishedAt = entity.finishedAt,
                        durationSeconds = entity.durationSeconds,
                        sets = emptyList()
                    )
                }
                DataResult.Success(sessions)
            }
            .catch { e ->
                e.rethrowIfCancellationOrFatal()
                emit(
                    if (e is SQLiteException) DataResult.Failure(DataError.Database(e))
                    else DataResult.Failure(DataError.Unknown(e))
                )
            }
    }

    override fun observeSession(id: Long): Flow<DataResult<WorkoutSession?>> {
        return workoutSessionDao.observeSessionWithSets(id)
            .map<WorkoutSessionWithSets?, DataResult<WorkoutSession?>> { relation ->
                DataResult.Success(relation?.toDomain())
            }
            .catch { e ->
                e.rethrowIfCancellationOrFatal()
                emit(
                    if (e is SQLiteException) DataResult.Failure(DataError.Database(e))
                    else DataResult.Failure(DataError.Unknown(e))
                )
            }
    }

    override suspend fun insertSession(session: WorkoutSession): DataResult<Long> {
        return try {
            val id = workoutSessionDao.insertSession(session.toEntity())
            DataResult.Success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            DataResult.Failure(DataError.Database(e))
        } catch (e: Exception) {
            DataResult.Failure(DataError.Unknown(e))
        }
    }

    override suspend fun updateSession(session: WorkoutSession): DataResult<Unit> {
        return try {
            workoutSessionDao.updateSession(session.toEntity())
            DataResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            DataResult.Failure(DataError.Database(e))
        } catch (e: Exception) {
            DataResult.Failure(DataError.Unknown(e))
        }
    }

    override suspend fun insertSet(setLog: SetLog): DataResult<Long> {
        return try {
            val id = setLogDao.insertSetLog(setLog.toEntity())
            DataResult.Success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            DataResult.Failure(DataError.Database(e))
        } catch (e: Exception) {
            DataResult.Failure(DataError.Unknown(e))
        }
    }

    override suspend fun deleteSession(sessionId: Long): DataResult<Unit> {
        return try {
            workoutSessionDao.deleteSession(sessionId)
            DataResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            DataResult.Failure(DataError.Database(e))
        } catch (e: Exception) {
            DataResult.Failure(DataError.Unknown(e))
        }
    }
}
