package com.haitranduc.fittrack.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutListViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutListUiState(isLoading = true))
    val uiState: StateFlow<WorkoutListUiState> = _uiState.asStateFlow()

    private val retryTrigger = MutableStateFlow(0)

    init {
        observeWorkouts()
    }

    private fun observeWorkouts() {
        viewModelScope.launch {
            retryTrigger.flatMapLatest {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                workoutRepository.observeWorkouts()
            }.collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                workouts = result.data,
                                errorMessage = null
                            )
                        }
                    }
                    is DataResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                workouts = emptyList(),
                                errorMessage = result.error.toString()
                            )
                        }
                    }
                }
            }
        }
    }

    fun retry() {
        retryTrigger.value++
    }

    fun onDeleteRequested(workout: Workout) {
        _uiState.update { it.copy(workoutToDelete = workout) }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(workoutToDelete = null) }
    }

    fun onDeleteConfirmed() {
        val currentState = _uiState.value
        val toDelete = currentState.workoutToDelete
        if (toDelete == null || currentState.isDeleting) return

        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (val result = workoutRepository.delete(toDelete.id)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            workoutToDelete = null,
                            isDeleting = false,
                            errorMessage = null
                        )
                    }
                }
                is DataResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }
}
