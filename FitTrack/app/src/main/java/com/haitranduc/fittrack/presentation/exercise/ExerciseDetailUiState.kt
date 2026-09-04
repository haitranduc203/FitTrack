package com.haitranduc.fittrack.presentation.exercise

import com.haitranduc.fittrack.domain.model.Exercise

data class ExerciseDetailUiState(
    val isLoading: Boolean = false,
    val exercise: Exercise? = null,
    val isMissing: Boolean = false,
    val errorMessage: String? = null
)
