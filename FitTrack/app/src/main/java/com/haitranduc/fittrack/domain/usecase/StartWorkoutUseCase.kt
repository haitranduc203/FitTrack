package com.haitranduc.fittrack.domain.usecase

import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import com.haitranduc.fittrack.domain.time.TimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val timeProvider: TimeProvider
) {
    private val mutex = Mutex()

    suspend operator fun invoke(workoutId: Long): StartWorkoutResult {
        return mutex.withLock {
            try {
                val workoutResult = workoutRepository.observeWorkout(workoutId).first()
                val workout = when (workoutResult) {
                    is DataResult.Success -> workoutResult.data ?: return StartWorkoutResult.WorkoutNotFound
                    is DataResult.Failure -> return StartWorkoutResult.Failure(workoutResult.error)
                }

                if (workout.exercises.isEmpty()) {
                    return StartWorkoutResult.EmptyWorkout
                }

                val activeSessionResult = workoutHistoryRepository.getActiveSession()
                when (activeSessionResult) {
                    is DataResult.Success -> {
                        val activeSession = activeSessionResult.data
                        if (activeSession != null) {
                            return StartWorkoutResult.ActiveSessionExists(activeSession.id)
                        }
                    }
                    is DataResult.Failure -> return StartWorkoutResult.Failure(activeSessionResult.error)
                }

                val now = timeProvider.currentTimeMillis()
                val session = WorkoutSession(
                    id = 0L,
                    workoutId = workout.id,
                    workoutNameSnapshot = workout.name,
                    startedAt = now,
                    finishedAt = null,
                    durationSeconds = null,
                    sets = emptyList()
                )

                when (val insertResult = workoutHistoryRepository.insertSession(session)) {
                    is DataResult.Success -> StartWorkoutResult.Success(insertResult.data)
                    is DataResult.Failure -> StartWorkoutResult.Failure(insertResult.error)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                StartWorkoutResult.Failure(DataError.Unknown(e))
            }
        }
    }
}
