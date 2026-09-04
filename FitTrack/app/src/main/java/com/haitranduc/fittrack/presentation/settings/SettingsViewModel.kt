package com.haitranduc.fittrack.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ThemePreferenceRepository
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.presentation.util.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferenceRepository: ThemePreferenceRepository
) : ViewModel() {

    private val isUpdatingTheme = MutableStateFlow(false)
    private val actionErrorMessage = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        themePreferenceRepository.observeThemePreference(),
        isUpdatingTheme,
        actionErrorMessage
    ) { prefResult, isUpdating, actionError ->
        when (prefResult) {
            is DataResult.Success -> SettingsUiState(
                isLoading = false,
                selectedTheme = prefResult.data,
                isUpdatingTheme = isUpdating,
                errorMessage = actionError
            )
            is DataResult.Failure -> SettingsUiState(
                isLoading = false,
                selectedTheme = ThemePreference.SYSTEM,
                isUpdatingTheme = isUpdating,
                errorMessage = actionError ?: prefResult.error.toUiText()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true)
    )

    fun onThemeSelected(preference: ThemePreference) {
        if (isUpdatingTheme.value || uiState.value.selectedTheme == preference) return
        isUpdatingTheme.value = true
        actionErrorMessage.value = null

        viewModelScope.launch {
            when (val result = themePreferenceRepository.setThemePreference(preference)) {
                is DataResult.Success -> {
                    // Update observed reactively via Flow
                }
                is DataResult.Failure -> {
                    actionErrorMessage.value = result.error.toUiText()
                }
            }
            isUpdatingTheme.value = false
        }
    }

    fun onClearError() {
        actionErrorMessage.value = null
    }
}
