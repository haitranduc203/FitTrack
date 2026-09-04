package com.haitranduc.fittrack.domain.usecase

import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import com.haitranduc.fittrack.domain.time.TimeProvider
import com.haitranduc.fittrack.domain.validation.SetResult
import com.haitranduc.fittrack.domain.validation.WorkoutValidation
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class CompleteSetUseCase @Inject constructor(
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        sessionId: Long,
        exerciseId: String,
        exerciseName: String,
        setNumber: Int,
        reps: Int,
        weightKg: Double
    ): CompleteSetResult {
        when (WorkoutValidation.validateSet(reps, weightKg)) {
            is SetResult.InvalidReps -> return CompleteSetResult.InvalidReps
            is SetResult.InvalidWeight -> return CompleteSetResult.InvalidWeight
            is SetResult.Valid -> { /* continue */ }
        }

        val sessionResult = workoutHistoryRepository.getSession(sessionId)
        val session = when (sessionResult) {
            is DataResult.Success -> sessionResult.data ?: return CompleteSetResult.SessionNotFound
            is DataResult.Failure -> return CompleteSetResult.Failure(sessionResult.error)
        }

        if (session.finishedAt != null) {
            return CompleteSetResult.SessionAlreadyFinished
        }

        val now = timeProvider.currentTimeMillis()
        val setLog = SetLog(
            id = 0L,
            sessionId = sessionId,
            exerciseId = exerciseId,
            exerciseNameSnapshot = exerciseName,
            setNumber = setNumber,
            reps = reps,
            weightKg = weightKg,
            completedAt = now
        )

        return try {
            when (val insertResult = workoutHistoryRepository.insertSet(setLog)) {
                is DataResult.Success -> CompleteSetResult.Success(insertResult.data)
                is DataResult.Failure -> CompleteSetResult.Failure(insertResult.error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CompleteSetResult.Failure(DataError.Unknown(e))
        }
    }
}
