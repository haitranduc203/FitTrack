package com.haitranduc.fittrack.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.StatisticsRepository
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import com.haitranduc.fittrack.presentation.util.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val statisticsRepository: StatisticsRepository
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
        _uiState.update { current -> current.copy(isLoading = current.sessions.isEmpty(), errorMessage = null) }

        observeJob = viewModelScope.launch {
            combine(
                workoutHistoryRepository.observeHistory(),
                statisticsRepository.observeTotalWorkouts(),
                statisticsRepository.observeTotalCompletedSets(),
                statisticsRepository.observeTotalTrainingTimeSeconds()
            ) { historyResult, workoutsResult, setsResult, timeResult ->
                when (historyResult) {
                    is DataResult.Success -> {
                        val totalWorkouts = when (workoutsResult) {
                            is DataResult.Success -> workoutsResult.data
                            is DataResult.Failure -> 0L
                        }
                        val totalSets = when (setsResult) {
                            is DataResult.Success -> setsResult.data
                            is DataResult.Failure -> 0L
                        }
                        val totalTime = when (timeResult) {
                            is DataResult.Success -> timeResult.data
                            is DataResult.Failure -> 0L
                        }
                        val error = when {
                            workoutsResult is DataResult.Failure -> workoutsResult.error.toUiText()
                            setsResult is DataResult.Failure -> setsResult.error.toUiText()
                            timeResult is DataResult.Failure -> timeResult.error.toUiText()
                            else -> null
                        }
                        HistoryUiState(
                            isLoading = false,
                            sessions = historyResult.data,
                            totalWorkouts = totalWorkouts,
                            totalCompletedSets = totalSets,
                            totalTrainingTimeSeconds = totalTime,
                            errorMessage = error
                        )
                    }
                    is DataResult.Failure -> {
                        HistoryUiState(
                            isLoading = false,
                            sessions = _uiState.value.sessions,
                            totalWorkouts = _uiState.value.totalWorkouts,
                            totalCompletedSets = _uiState.value.totalCompletedSets,
                            totalTrainingTimeSeconds = _uiState.value.totalTrainingTimeSeconds,
                            errorMessage = historyResult.error.toUiText()
                        )
                    }
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
