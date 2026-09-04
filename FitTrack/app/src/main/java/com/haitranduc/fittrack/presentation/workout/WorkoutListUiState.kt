package com.haitranduc.fittrack.presentation.workout

import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.presentation.util.UiText

data class WorkoutListUiState(
    val isLoading: Boolean = true,
    val workouts: List<Workout> = emptyList(),
    val workoutToDelete: Workout? = null,
    val isDeleting: Boolean = false,
    val errorMessage: UiText? = null
)
