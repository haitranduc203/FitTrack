package com.haitranduc.fittrack.domain.usecase

import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import com.haitranduc.fittrack.domain.time.TimeProvider
import com.haitranduc.fittrack.domain.validation.ExerciseListResult
import com.haitranduc.fittrack.domain.validation.NameResult
import com.haitranduc.fittrack.domain.validation.WorkoutValidation
import javax.inject.Inject

sealed interface SaveWorkoutResult {
    data class Success(val workoutId: Long) : SaveWorkoutResult
    data class InvalidName(val reason: NameResult) : SaveWorkoutResult
    data class InvalidExercises(val reason: ExerciseListResult) : SaveWorkoutResult
    data class RepositoryError(val error: DataError) : SaveWorkoutResult
}

class SaveWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val timeProvider: TimeProvider
) {

    suspend operator fun invoke(workout: Workout): SaveWorkoutResult {
        val nameValidation = WorkoutValidation.validateName(workout.name)
        if (nameValidation !is NameResult.Valid) {
            return SaveWorkoutResult.InvalidName(nameValidation)
        }

        val exerciseIds = workout.exercises.map { it.id }
        val exerciseValidation = WorkoutValidation.validateExerciseIds(exerciseIds)
        if (exerciseValidation !is ExerciseListResult.Valid) {
            return SaveWorkoutResult.InvalidExercises(exerciseValidation)
        }

        val now = timeProvider.currentTimeMillis()
        val toSave = if (workout.id == 0L) {
            workout.copy(
                name = nameValidation.trimmedName,
                createdAt = if (workout.createdAt == 0L) now else workout.createdAt,
                updatedAt = now
            )
        } else {
            workout.copy(
                name = nameValidation.trimmedName,
                updatedAt = now
            )
        }

        return when (val saveResult = workoutRepository.save(toSave)) {
            is DataResult.Success -> SaveWorkoutResult.Success(saveResult.data)
            is DataResult.Failure -> SaveWorkoutResult.RepositoryError(saveResult.error)
        }
    }
}
