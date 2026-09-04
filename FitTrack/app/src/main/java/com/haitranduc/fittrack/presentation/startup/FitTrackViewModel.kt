package com.haitranduc.fittrack.presentation.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import com.haitranduc.fittrack.domain.repository.SeedImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FitTrackViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = _uiState.asStateFlow()

    init {
        ensureSeeded()
    }

    fun retry() {
        ensureSeeded()
    }

    private fun ensureSeeded() {
        viewModelScope.launch {
            _uiState.value = StartupUiState.Loading
            when (val result = exerciseRepository.ensureSeeded()) {
                is SeedImportResult.Imported,
                is SeedImportResult.AlreadySeeded -> {
                    _uiState.value = StartupUiState.Ready
                }
                is SeedImportResult.Failure -> {
                    _uiState.value = StartupUiState.Error(result.error.toString())
                }
            }
        }
    }
}
