package com.haitranduc.fittrack.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.presentation.util.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WorkoutListEvent {
    data class ShowSnackbar(val message: UiText) : WorkoutListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    companion object {
        const val KEY_WORKOUT_SAVED = "workout_saved"
    }

    private val _uiState = MutableStateFlow(WorkoutListUiState(isLoading = true))
    val uiState: StateFlow<WorkoutListUiState> = _uiState.asStateFlow()

    private val _events = Channel<WorkoutListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val retryTrigger = MutableStateFlow(0)

    init {
        observeWorkouts()
        observeSavedState()
    }

    private fun observeSavedState() {
        viewModelScope.launch {
            savedStateHandle.getStateFlow(KEY_WORKOUT_SAVED, false).collect { wasSaved ->
                if (wasSaved) {
                    savedStateHandle.remove<Boolean>(KEY_WORKOUT_SAVED)
                    _events.send(WorkoutListEvent.ShowSnackbar(UiText.StringResource(R.string.msg_workout_saved)))
                }
            }
        }
    }

    private fun observeWorkouts() {
        viewModelScope.launch {
            retryTrigger.flatMapLatest {
                _uiState.update { current ->
                    current.copy(
                        isLoading = current.workouts.isEmpty(),
                        errorMessage = null
                    )
                }
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
                                errorMessage = result.error.toUiText()
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
            try {
                when (val result = workoutRepository.delete(toDelete.id)) {
                    is DataResult.Success -> {
                        _uiState.update {
                            it.copy(
                                workoutToDelete = null,
                                errorMessage = null
                            )
                        }
                        _events.send(WorkoutListEvent.ShowSnackbar(UiText.StringResource(R.string.msg_workout_deleted)))
                    }
                    is DataResult.Failure -> {
                        val errorText = result.error.toUiText()
                        _uiState.update {
                            it.copy(
                                errorMessage = errorText
                            )
                        }
                        _events.send(WorkoutListEvent.ShowSnackbar(errorText))
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: java.lang.Error) {
                throw e
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }
}
