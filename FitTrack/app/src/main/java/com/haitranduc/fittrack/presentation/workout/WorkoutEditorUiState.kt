package com.haitranduc.fittrack.presentation.workout

import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.presentation.util.UiText

data class WorkoutEditorUiState(
    val isLoading: Boolean = false,
    val isMissing: Boolean = false,
    val isSaving: Boolean = false,
    val workoutId: Long? = null,
    val workoutName: String = "",
    val nameErrorRes: Int? = null,
    val exercises: List<Exercise> = emptyList(),
    val exerciseErrorRes: Int? = null,
    val isPickerOpen: Boolean = false,
    val pickerExercises: List<Exercise> = emptyList(),
    val pickerQuery: String = "",
    val errorMessage: UiText? = null
)

sealed interface WorkoutEditorEvent {
    data class NavigateBack(val workoutId: Long) : WorkoutEditorEvent
    data class NavigateToActiveWorkout(val sessionId: Long) : WorkoutEditorEvent
    data class ShowSnackbar(val message: UiText) : WorkoutEditorEvent
}
