package com.haitranduc.fittrack.presentation.exercise

import com.haitranduc.fittrack.domain.model.Exercise

data class ExerciseListUiState(
    val isLoading: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val selectedBodyPart: String? = null,
    val selectedEquipment: String? = null,
    val errorMessage: String? = null
)
