package com.haitranduc.fittrack.domain.validation

object WorkoutValidation {
    const val MAX_NAME_LENGTH = 50
    const val MIN_REPS = 1
    const val MAX_REPS = 100
    const val MIN_WEIGHT_KG = 0.0
    const val MAX_WEIGHT_KG = 1000.0

    fun validateName(rawName: String): NameResult {
        val trimmed = rawName.trim()
        return when {
            trimmed.isEmpty() -> NameResult.Blank
            trimmed.length > MAX_NAME_LENGTH -> NameResult.TooLong
            else -> NameResult.Valid(trimmed)
        }
    }

    fun validateExerciseIds(exerciseIds: List<String>): ExerciseListResult {
        if (exerciseIds.isEmpty()) {
            return ExerciseListResult.Empty
        }
        val seen = HashSet<String>(exerciseIds.size)
        for (id in exerciseIds) {
            if (!seen.add(id)) {
                return ExerciseListResult.Duplicate(id)
            }
        }
        return ExerciseListResult.Valid
    }

    fun validateSet(reps: Int, weightKg: Double): SetResult {
        if (reps < MIN_REPS || reps > MAX_REPS) {
            return SetResult.InvalidReps
        }
        if (!weightKg.isFinite() || weightKg < MIN_WEIGHT_KG || weightKg > MAX_WEIGHT_KG) {
            return SetResult.InvalidWeight
        }
        return SetResult.Valid
    }
}

sealed interface NameResult {
    data class Valid(val trimmedName: String) : NameResult
    data object Blank : NameResult
    data object TooLong : NameResult
}

sealed interface ExerciseListResult {
    data object Valid : ExerciseListResult
    data object Empty : ExerciseListResult
    data class Duplicate(val exerciseId: String) : ExerciseListResult
}

sealed interface SetResult {
    data object Valid : SetResult
    data object InvalidReps : SetResult
    data object InvalidWeight : SetResult
}
