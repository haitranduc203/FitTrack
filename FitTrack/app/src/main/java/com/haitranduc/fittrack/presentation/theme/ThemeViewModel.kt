package com.haitranduc.fittrack.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ThemePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    themePreferenceRepository: ThemePreferenceRepository
) : ViewModel() {

    val themePreference: StateFlow<ThemePreference> = themePreferenceRepository
        .observeThemePreference()
        .map { result ->
            when (result) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> ThemePreference.SYSTEM
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreference.SYSTEM
        )
}
