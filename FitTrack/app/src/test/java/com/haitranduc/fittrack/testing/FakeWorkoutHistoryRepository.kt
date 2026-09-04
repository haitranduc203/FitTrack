package com.haitranduc.fittrack.testing

import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

open class FakeWorkoutHistoryRepository : WorkoutHistoryRepository {

    val sessions = mutableListOf<WorkoutSession>()
    val setLogs = mutableListOf<SetLog>()
    private val sessionsFlow = MutableStateFlow<List<WorkoutSession>>(emptyList())

    var insertSessionError: DataError? = null
    var updateSessionError: DataError? = null
    var insertSetError: DataError? = null
    var deleteSessionError: DataError? = null
    var getActiveSessionError: DataError? = null
    var getSessionError: DataError? = null
    var observeHistoryError: DataError? = null
    var observeSessionError: DataError? = null

    private var nextSessionId = 1L
    private var nextSetId = 1L

    fun refreshFlow() {
        sessionsFlow.value = sessions.map { sessionWithSets(it) }
    }

    private fun sessionWithSets(session: WorkoutSession): WorkoutSession {
        val sets = setLogs.filter { it.sessionId == session.id }
            .sortedWith(compareBy({ it.exerciseId }, { it.setNumber }, { it.id }))
        return session.copy(sets = sets)
    }

    override fun observeHistory(): Flow<DataResult<List<WorkoutSession>>> {
        return sessionsFlow.map { list ->
            observeHistoryError?.let { DataResult.Failure(it) }
                ?: DataResult.Success(list.filter { it.finishedAt != null })
        }
    }

    override fun observeSession(id: Long): Flow<DataResult<WorkoutSession?>> {
        return sessionsFlow.map { list ->
            observeSessionError?.let { DataResult.Failure(it) }
                ?: DataResult.Success(list.find { it.id == id })
        }
    }

    override suspend fun getActiveSession(): DataResult<WorkoutSession?> {
        getActiveSessionError?.let { return DataResult.Failure(it) }
        val active = sessions.find { it.finishedAt == null }
        return DataResult.Success(active?.let { sessionWithSets(it) })
    }

    override suspend fun getSession(id: Long): DataResult<WorkoutSession?> {
        getSessionError?.let { return DataResult.Failure(it) }
        val session = sessions.find { it.id == id }
        return DataResult.Success(session?.let { sessionWithSets(it) })
    }

    override suspend fun insertSession(session: WorkoutSession): DataResult<Long> {
        insertSessionError?.let { return DataResult.Failure(it) }
        val id = if (session.id == 0L) nextSessionId++ else session.id
        val newSession = session.copy(id = id)
        sessions.add(newSession)
        refreshFlow()
        return DataResult.Success(id)
    }

    override suspend fun updateSession(session: WorkoutSession): DataResult<Unit> {
        updateSessionError?.let { return DataResult.Failure(it) }
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            sessions[index] = session
        } else {
            sessions.add(session)
        }
        refreshFlow()
        return DataResult.Success(Unit)
    }

    override suspend fun insertSet(setLog: SetLog): DataResult<Long> {
        insertSetError?.let { return DataResult.Failure(it) }
        val id = if (setLog.id == 0L) nextSetId++ else setLog.id
        val newSet = setLog.copy(id = id)
        setLogs.add(newSet)
        refreshFlow()
        return DataResult.Success(id)
    }

    override suspend fun deleteSession(sessionId: Long): DataResult<Unit> {
        deleteSessionError?.let { return DataResult.Failure(it) }
        sessions.removeAll { it.id == sessionId }
        setLogs.removeAll { it.sessionId == sessionId }
        refreshFlow()
        return DataResult.Success(Unit)
    }
}
