package com.haitranduc.fittrack.domain.usecase

import com.haitranduc.fittrack.domain.repository.DataError

sealed interface StartWorkoutResult {
    data class Success(val sessionId: Long) : StartWorkoutResult
    data object WorkoutNotFound : StartWorkoutResult
    data object EmptyWorkout : StartWorkoutResult
    data class ActiveSessionExists(val sessionId: Long) : StartWorkoutResult
    data class Failure(val error: DataError) : StartWorkoutResult
}

sealed interface CompleteSetResult {
    data class Success(val setId: Long) : CompleteSetResult
    data object InvalidReps : CompleteSetResult
    data object InvalidWeight : CompleteSetResult
    data object SessionNotFound : CompleteSetResult
    data object SessionAlreadyFinished : CompleteSetResult
    data class Failure(val error: DataError) : CompleteSetResult
}

sealed interface FinishWorkoutResult {
    data class Success(val sessionId: Long) : FinishWorkoutResult
    data object SessionNotFound : FinishWorkoutResult
    data object SessionAlreadyFinished : FinishWorkoutResult
    data object NoSetsCompleted : FinishWorkoutResult
    data class Failure(val error: DataError) : FinishWorkoutResult
}
