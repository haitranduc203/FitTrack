package com.haitranduc.fittrack.presentation.settings

import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.presentation.util.UiText

data class SettingsUiState(
    val isLoading: Boolean = false,
    val selectedTheme: ThemePreference = ThemePreference.SYSTEM,
    val isUpdatingTheme: Boolean = false,
    val errorMessage: UiText? = null
)
