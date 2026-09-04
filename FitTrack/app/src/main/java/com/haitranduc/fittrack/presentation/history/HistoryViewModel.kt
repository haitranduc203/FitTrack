package com.haitranduc.fittrack.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutHistoryRepository: WorkoutHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadHistory()
    }

    fun onRetry() {
        loadHistory()
    }

    private fun loadHistory() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        observeJob = viewModelScope.launch {
            workoutHistoryRepository.observeHistory().collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                sessions = result.data,
                                errorMessage = null
                            )
                        }
                    }
                    is DataResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.error.toString()
                            )
                        }
                    }
                }
            }
        }
    }
}
