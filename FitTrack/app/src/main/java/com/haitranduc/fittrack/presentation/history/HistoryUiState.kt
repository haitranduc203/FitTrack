package com.haitranduc.fittrack.presentation.history

import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.presentation.util.UiText

data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<WorkoutSession> = emptyList(),
    val totalWorkouts: Long = 0L,
    val errorMessage: UiText? = null
)
