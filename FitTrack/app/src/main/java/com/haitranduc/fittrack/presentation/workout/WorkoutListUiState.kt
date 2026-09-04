package com.haitranduc.fittrack.presentation.workout

import com.haitranduc.fittrack.domain.model.Workout

data class WorkoutListUiState(
    val isLoading: Boolean = true,
    val workouts: List<Workout> = emptyList(),
    val workoutToDelete: Workout? = null,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)
