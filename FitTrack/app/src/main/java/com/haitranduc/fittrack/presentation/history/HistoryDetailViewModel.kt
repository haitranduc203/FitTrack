package com.haitranduc.fittrack.presentation.history

import androidx.lifecycle.SavedStateHandle
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
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutHistoryRepository: WorkoutHistoryRepository
) : ViewModel() {

    private val parsedSessionId: Long? = run {
        val raw = savedStateHandle.get<Any>("sessionId")
        when (raw) {
            is Long -> raw
            is String -> raw.toLongOrNull()
            is Number -> raw.toLong()
            else -> null
        }
    }

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadSession()
    }

    fun onRetry() {
        loadSession()
    }

    private fun loadSession() {
        val sid = parsedSessionId
        if (sid == null || sid <= 0L) {
            _uiState.update { it.copy(isLoading = false, isMissing = true) }
            return
        }

        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        observeJob = viewModelScope.launch {
            workoutHistoryRepository.observeSession(sid).collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        val session = result.data
                        if (session == null) {
                            _uiState.update { it.copy(isLoading = false, isMissing = true, session = null) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isMissing = false,
                                    session = session,
                                    errorMessage = null
                                )
                            }
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
