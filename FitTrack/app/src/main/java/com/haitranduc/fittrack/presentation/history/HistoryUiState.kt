package com.haitranduc.fittrack.presentation.history

import com.haitranduc.fittrack.domain.model.WorkoutSession

data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<WorkoutSession> = emptyList(),
    val errorMessage: String? = null
)
