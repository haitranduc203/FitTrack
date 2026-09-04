package com.haitranduc.fittrack.domain.usecase

import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import com.haitranduc.fittrack.domain.time.TimeProvider
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class FinishWorkoutUseCase @Inject constructor(
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(sessionId: Long): FinishWorkoutResult {
        val sessionResult = workoutHistoryRepository.getSession(sessionId)
        val session = when (sessionResult) {
            is DataResult.Success -> sessionResult.data ?: return FinishWorkoutResult.SessionNotFound
            is DataResult.Failure -> return FinishWorkoutResult.Failure(sessionResult.error)
        }

        if (session.finishedAt != null) {
            return FinishWorkoutResult.SessionAlreadyFinished
        }

        if (session.sets.isEmpty()) {
            return FinishWorkoutResult.NoSetsCompleted
        }

        val finishedAt = timeProvider.currentTimeMillis()
        val durationSeconds = maxOf(0L, (finishedAt - session.startedAt) / 1000L)
        val updatedSession = session.copy(
            finishedAt = finishedAt,
            durationSeconds = durationSeconds
        )

        return try {
            when (val updateResult = workoutHistoryRepository.updateSession(updatedSession)) {
                is DataResult.Success -> FinishWorkoutResult.Success(sessionId)
                is DataResult.Failure -> FinishWorkoutResult.Failure(updateResult.error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FinishWorkoutResult.Failure(DataError.Unknown(e))
        }
    }
}
