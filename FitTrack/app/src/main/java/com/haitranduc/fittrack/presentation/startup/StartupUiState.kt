package com.haitranduc.fittrack.presentation.startup

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data object Ready : StartupUiState
    data class Error(val message: String? = null) : StartupUiState
}
