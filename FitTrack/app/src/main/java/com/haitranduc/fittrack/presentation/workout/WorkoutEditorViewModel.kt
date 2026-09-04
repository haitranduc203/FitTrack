package com.haitranduc.fittrack.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import com.haitranduc.fittrack.domain.usecase.SaveWorkoutResult
import com.haitranduc.fittrack.domain.usecase.SaveWorkoutUseCase
import com.haitranduc.fittrack.domain.usecase.StartWorkoutResult
import com.haitranduc.fittrack.domain.usecase.StartWorkoutUseCase
import com.haitranduc.fittrack.domain.validation.ExerciseListResult
import com.haitranduc.fittrack.domain.validation.NameResult
import com.haitranduc.fittrack.domain.validation.WorkoutValidation
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.presentation.util.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val saveWorkoutUseCase: SaveWorkoutUseCase,
    private val startWorkoutUseCase: StartWorkoutUseCase
) : ViewModel() {

    private val rawWorkoutId: String? = savedStateHandle.get<String>("workoutId")
    private var originalCreatedAt: Long = 0L

    private val _uiState = MutableStateFlow(WorkoutEditorUiState())
    val uiState: StateFlow<WorkoutEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<WorkoutEditorEvent>(Channel.BUFFERED)
    val events: Flow<WorkoutEditorEvent> = _events.receiveAsFlow()

    private val pickerQueryFlow = MutableStateFlow("")

    init {
        observePickerExercises()
        initializeWorkout()
    }

    private fun initializeWorkout() {
        if (rawWorkoutId == null) {
            // Create mode
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isMissing = false,
                    workoutId = null,
                    workoutName = "",
                    exercises = emptyList()
                )
            }
        } else {
            val parsedId = rawWorkoutId.toLongOrNull()
            if (parsedId == null || parsedId <= 0L) {
                _uiState.update { it.copy(isLoading = false, isMissing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true, isMissing = false, workoutId = parsedId) }
                viewModelScope.launch {
                    workoutRepository.observeWorkout(parsedId).collect { result ->
                        when (result) {
                            is DataResult.Success -> {
                                val workout = result.data
                                if (workout == null) {
                                    _uiState.update { it.copy(isLoading = false, isMissing = true) }
                                } else {
                                    originalCreatedAt = workout.createdAt
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            isMissing = false,
                                            workoutId = workout.id,
                                            workoutName = workout.name,
                                            exercises = workout.exercises,
                                            errorMessage = null
                                        )
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
        }
    }

    private fun observePickerExercises() {
        viewModelScope.launch {
            pickerQueryFlow.flatMapLatest { query ->
                exerciseRepository.observeExercises(query = query.trim())
            }.collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        _uiState.update { it.copy(pickerExercises = result.data) }
                    }
                    is DataResult.Failure -> {
                        _uiState.update { it.copy(errorMessage = result.error.toUiText()) }
                    }
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(workoutName = name, nameErrorRes = null)
        }
    }

    fun openExercisePicker() {
        _uiState.update { it.copy(isPickerOpen = true) }
    }

    fun closeExercisePicker() {
        _uiState.update { it.copy(isPickerOpen = false) }
    }

    fun onPickerQueryChanged(query: String) {
        pickerQueryFlow.value = query
        _uiState.update { it.copy(pickerQuery = query) }
    }

    fun onExerciseSelected(exercise: Exercise) {
        val currentList = _uiState.value.exercises
        if (currentList.any { it.id == exercise.id }) {
            _uiState.update {
                it.copy(exerciseErrorRes = R.string.error_workout_exercise_duplicate)
            }
        } else {
            _uiState.update {
                it.copy(
                    exercises = currentList + exercise,
                    exerciseErrorRes = null,
                    isPickerOpen = false
                )
            }
        }
    }

    fun onRemoveExercise(index: Int) {
        val currentList = _uiState.value.exercises
        if (index in currentList.indices) {
            val updated = currentList.toMutableList().apply { removeAt(index) }
            _uiState.update { it.copy(exercises = updated) }
        }
    }

    fun onMoveExerciseUp(index: Int) {
        val currentList = _uiState.value.exercises.toMutableList()
        if (index > 0 && index < currentList.size) {
            val item = currentList.removeAt(index)
            currentList.add(index - 1, item)
            _uiState.update { it.copy(exercises = currentList) }
        }
    }

    fun onMoveExerciseDown(index: Int) {
        val currentList = _uiState.value.exercises.toMutableList()
        if (index >= 0 && index < currentList.size - 1) {
            val item = currentList.removeAt(index)
            currentList.add(index + 1, item)
            _uiState.update { it.copy(exercises = currentList) }
        }
    }

    fun onSaveClicked() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        val nameValidation = WorkoutValidation.validateName(currentState.workoutName)
        val nameError = when (nameValidation) {
            is NameResult.Valid -> null
            NameResult.Blank -> R.string.error_workout_name_blank
            NameResult.TooLong -> R.string.error_workout_name_too_long
        }

        val exerciseValidation = WorkoutValidation.validateExerciseIds(currentState.exercises.map { it.id })
        val exerciseError = when (exerciseValidation) {
            ExerciseListResult.Valid -> null
            ExerciseListResult.Empty -> R.string.error_workout_exercises_empty
            is ExerciseListResult.Duplicate -> R.string.error_workout_exercise_duplicate
        }

        if (nameError != null || exerciseError != null) {
            _uiState.update {
                it.copy(nameErrorRes = nameError, exerciseErrorRes = exerciseError)
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val workout = Workout(
                id = currentState.workoutId ?: 0L,
                name = currentState.workoutName,
                createdAt = originalCreatedAt,
                updatedAt = 0L,
                exercises = currentState.exercises
            )
            when (val result = saveWorkoutUseCase(workout)) {
                is SaveWorkoutResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(WorkoutEditorEvent.NavigateBack(result.workoutId))
                }
                is SaveWorkoutResult.InvalidName -> {
                    val resId = when (result.reason) {
                        NameResult.Blank -> R.string.error_workout_name_blank
                        NameResult.TooLong -> R.string.error_workout_name_too_long
                        is NameResult.Valid -> null
                    }
                    _uiState.update { it.copy(isSaving = false, nameErrorRes = resId) }
                }
                is SaveWorkoutResult.InvalidExercises -> {
                    val resId = when (result.reason) {
                        ExerciseListResult.Empty -> R.string.error_workout_exercises_empty
                        is ExerciseListResult.Duplicate -> R.string.error_workout_exercise_duplicate
                        ExerciseListResult.Valid -> null
                    }
                    _uiState.update { it.copy(isSaving = false, exerciseErrorRes = resId) }
                }
                is SaveWorkoutResult.RepositoryError -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.error.toUiText())
                    }
                }
            }
        }
    }

    fun onStartClicked() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        val nameValidation = WorkoutValidation.validateName(currentState.workoutName)
        val nameError = when (nameValidation) {
            is NameResult.Valid -> null
            NameResult.Blank -> R.string.error_workout_name_blank
            NameResult.TooLong -> R.string.error_workout_name_too_long
        }

        val exerciseValidation = WorkoutValidation.validateExerciseIds(currentState.exercises.map { it.id })
        val exerciseError = when (exerciseValidation) {
            ExerciseListResult.Valid -> null
            ExerciseListResult.Empty -> R.string.error_workout_exercises_empty
            is ExerciseListResult.Duplicate -> R.string.error_workout_exercise_duplicate
        }

        if (nameError != null || exerciseError != null) {
            _uiState.update {
                it.copy(nameErrorRes = nameError, exerciseErrorRes = exerciseError)
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val workout = Workout(
                id = currentState.workoutId ?: 0L,
                name = currentState.workoutName,
                createdAt = originalCreatedAt,
                updatedAt = 0L,
                exercises = currentState.exercises
            )
            when (val saveResult = saveWorkoutUseCase(workout)) {
                is SaveWorkoutResult.Success -> {
                    val workoutId = saveResult.workoutId
                    when (val startResult = startWorkoutUseCase(workoutId)) {
                        is StartWorkoutResult.Success -> {
                            _uiState.update { it.copy(isSaving = false) }
                            _events.send(WorkoutEditorEvent.NavigateToActiveWorkout(startResult.sessionId))
                        }
                        is StartWorkoutResult.ActiveSessionExists -> {
                            _uiState.update { it.copy(isSaving = false, errorMessage = null) }
                            _events.send(WorkoutEditorEvent.NavigateToActiveWorkout(startResult.sessionId))
                        }
                        StartWorkoutResult.WorkoutNotFound -> {
                            _uiState.update {
                                it.copy(isSaving = false, errorMessage = UiText.StringResource(R.string.error_workout_not_found))
                            }
                        }
                        StartWorkoutResult.EmptyWorkout -> {
                            _uiState.update {
                                it.copy(isSaving = false, exerciseErrorRes = R.string.error_workout_exercises_empty)
                            }
                        }
                        is StartWorkoutResult.Failure -> {
                            _uiState.update {
                                it.copy(isSaving = false, errorMessage = startResult.error.toUiText())
                            }
                        }
                    }
                }
                is SaveWorkoutResult.InvalidName -> {
                    val resId = when (saveResult.reason) {
                        NameResult.Blank -> R.string.error_workout_name_blank
                        NameResult.TooLong -> R.string.error_workout_name_too_long
                        is NameResult.Valid -> null
                    }
                    _uiState.update { it.copy(isSaving = false, nameErrorRes = resId) }
                }
                is SaveWorkoutResult.InvalidExercises -> {
                    val resId = when (saveResult.reason) {
                        ExerciseListResult.Empty -> R.string.error_workout_exercises_empty
                        is ExerciseListResult.Duplicate -> R.string.error_workout_exercise_duplicate
                        ExerciseListResult.Valid -> null
                    }
                    _uiState.update { it.copy(isSaving = false, exerciseErrorRes = resId) }
                }
                is SaveWorkoutResult.RepositoryError -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = saveResult.error.toUiText())
                    }
                }
            }
        }
    }
}
