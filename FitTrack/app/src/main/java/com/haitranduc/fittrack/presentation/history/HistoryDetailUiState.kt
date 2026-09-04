package com.haitranduc.fittrack.presentation.history

import com.haitranduc.fittrack.domain.model.WorkoutSession

data class HistoryDetailUiState(
    val isLoading: Boolean = true,
    val isMissing: Boolean = false,
    val session: WorkoutSession? = null,
    val errorMessage: String? = null
)
