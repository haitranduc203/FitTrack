package com.haitranduc.fittrack.presentation.startup

import com.haitranduc.fittrack.presentation.util.UiText

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data object Ready : StartupUiState
    data class Error(val message: UiText? = null) : StartupUiState
}
