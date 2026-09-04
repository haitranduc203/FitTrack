package com.haitranduc.fittrack.presentation.activeworkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import com.haitranduc.fittrack.domain.time.TimeProvider
import com.haitranduc.fittrack.domain.usecase.CompleteSetResult
import com.haitranduc.fittrack.domain.usecase.CompleteSetUseCase
import com.haitranduc.fittrack.domain.usecase.FinishWorkoutResult
import com.haitranduc.fittrack.domain.usecase.FinishWorkoutUseCase
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.presentation.util.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val workoutRepository: WorkoutRepository,
    private val completeSetUseCase: CompleteSetUseCase,
    private val finishWorkoutUseCase: FinishWorkoutUseCase,
    private val timeProvider: TimeProvider
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

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    init {
        val sid = parsedSessionId
        if (sid == null || sid <= 0L) {
            _uiState.update { it.copy(isLoading = false, isMissing = true) }
        } else {
            observeSession(sid)
        }
    }

    private fun observeSession(sessionId: Long) {
        viewModelScope.launch {
            workoutHistoryRepository.observeSession(sessionId).collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        val session = result.data
                        if (session == null) {
                            _uiState.update { it.copy(isLoading = false, isMissing = true) }
                        } else {
                            val elapsed = maxOf(0L, (timeProvider.currentTimeMillis() - session.startedAt) / 1000L)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isMissing = false,
                                    session = session,
                                    completedSets = session.sets,
                                    elapsedTimeSeconds = elapsed
                                )
                            }
                            if (_uiState.value.exercises.isEmpty() && session.workoutId != null) {
                                loadExercises(session.workoutId)
                            }
                        }
                    }
                    is DataResult.Failure -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.error.toUiText())
                        }
                    }
                }
            }
        }
    }

    private fun loadExercises(workoutId: Long) {
        viewModelScope.launch {
            val workoutResult = workoutRepository.observeWorkout(workoutId).first()
            if (workoutResult is DataResult.Success) {
                val exercises = workoutResult.data?.exercises ?: emptyList()
                _uiState.update { it.copy(exercises = exercises) }
            }
        }
    }

    fun onTimerTick() {
        val session = _uiState.value.session
        if (session != null && session.finishedAt == null) {
            val elapsed = maxOf(0L, (timeProvider.currentTimeMillis() - session.startedAt) / 1000L)
            _uiState.update { it.copy(elapsedTimeSeconds = elapsed) }
        }
    }

    fun onRepsChanged(exerciseId: String, reps: String) {
        _uiState.update {
            it.copy(inputReps = it.inputReps + (exerciseId to reps))
        }
    }

    fun onWeightChanged(exerciseId: String, weight: String) {
        _uiState.update {
            it.copy(inputWeight = it.inputWeight + (exerciseId to weight))
        }
    }

    fun onCompleteSetClicked(exercise: Exercise) {
        if (_uiState.value.isCompletingSet) return
        val sid = parsedSessionId ?: return

        val repsStr = _uiState.value.inputReps[exercise.id] ?: "10"
        val weightStr = _uiState.value.inputWeight[exercise.id] ?: "50"
        val reps = repsStr.toIntOrNull()
        val weight = weightStr.toDoubleOrNull()

        if (reps == null) {
            _uiState.update {
                it.copy(inputErrors = it.inputErrors + (exercise.id to UiText.StringResource(R.string.error_invalid_reps_number)))
            }
            return
        }

        if (weight == null) {
            _uiState.update {
                it.copy(inputErrors = it.inputErrors + (exercise.id to UiText.StringResource(R.string.error_invalid_weight_number)))
            }
            return
        }

        val existingSets = _uiState.value.completedSets.filter { it.exerciseId == exercise.id }
        val nextSetNumber = (existingSets.maxOfOrNull { it.setNumber } ?: 0) + 1

        _uiState.update { it.copy(isCompletingSet = true) }

        viewModelScope.launch {
            when (val res = completeSetUseCase(sid, exercise.id, exercise.name, nextSetNumber, reps, weight)) {
                is CompleteSetResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCompletingSet = false,
                            inputErrors = it.inputErrors - exercise.id
                        )
                    }
                }
                CompleteSetResult.InvalidReps -> {
                    _uiState.update {
                        it.copy(
                            isCompletingSet = false,
                            inputErrors = it.inputErrors + (exercise.id to UiText.StringResource(R.string.error_invalid_reps))
                        )
                    }
                }
                CompleteSetResult.InvalidWeight -> {
                    _uiState.update {
                        it.copy(
                            isCompletingSet = false,
                            inputErrors = it.inputErrors + (exercise.id to UiText.StringResource(R.string.error_invalid_weight))
                        )
                    }
                }
                CompleteSetResult.SessionNotFound -> {
                    _uiState.update {
                        it.copy(isCompletingSet = false, isMissing = true)
                    }
                }
                CompleteSetResult.SessionAlreadyFinished -> {
                    _uiState.update {
                        it.copy(isCompletingSet = false, errorMessage = UiText.StringResource(R.string.error_session_already_finished))
                    }
                }
                is CompleteSetResult.Failure -> {
                    _uiState.update {
                        it.copy(isCompletingSet = false, errorMessage = res.error.toUiText())
                    }
                }
            }
        }
    }

    fun onFinishClicked() {
        if (_uiState.value.isFinishing) return
        val sid = parsedSessionId ?: return

        _uiState.update { it.copy(isFinishing = true, finishError = null) }

        viewModelScope.launch {
            when (val res = finishWorkoutUseCase(sid)) {
                is FinishWorkoutResult.Success -> {
                    _uiState.update {
                        it.copy(isFinishing = false, finishedSessionId = res.sessionId)
                    }
                }
                FinishWorkoutResult.NoSetsCompleted -> {
                    _uiState.update {
                        it.copy(
                            isFinishing = false,
                            finishError = UiText.StringResource(R.string.error_no_completed_sets)
                        )
                    }
                }
                FinishWorkoutResult.SessionAlreadyFinished -> {
                    _uiState.update {
                        it.copy(isFinishing = false, finishedSessionId = sid)
                    }
                }
                FinishWorkoutResult.SessionNotFound -> {
                    _uiState.update {
                        it.copy(isFinishing = false, finishError = UiText.StringResource(R.string.error_session_not_found))
                    }
                }
                is FinishWorkoutResult.Failure -> {
                    _uiState.update {
                        it.copy(isFinishing = false, finishError = res.error.toUiText())
                    }
                }
            }
        }
    }
}
