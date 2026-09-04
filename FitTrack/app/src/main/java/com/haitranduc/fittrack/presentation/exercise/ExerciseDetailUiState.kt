package com.haitranduc.fittrack.presentation.exercise

import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.presentation.util.UiText

data class ExerciseDetailUiState(
    val isLoading: Boolean = false,
    val exercise: Exercise? = null,
    val isMissing: Boolean = false,
    val errorMessage: UiText? = null
)
