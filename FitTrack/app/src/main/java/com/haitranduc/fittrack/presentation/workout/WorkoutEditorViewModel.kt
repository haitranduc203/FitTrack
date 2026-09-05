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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val saveWorkoutUseCase: SaveWorkoutUseCase,
    private val startWorkoutUseCase: StartWorkoutUseCase
) : ViewModel() {

    companion object {
        const val KEY_DRAFT_NAME = "draft_workout_name"
        const val KEY_DRAFT_EXERCISE_IDS = "draft_exercise_ids"
        const val KEY_DRAFT_EXERCISES_EMPTY = "draft_exercises_empty"
    }

    private val rawWorkoutId: String? = savedStateHandle.get<String>("workoutId")
    private var originalCreatedAt: Long = 0L

    private var initialWorkoutName: String = ""
    private var initialExercises: List<Exercise> = emptyList()
    private var isInitialized: Boolean = false

    private fun checkHasChanges(name: String, exercises: List<Exercise>): Boolean {
        if (!isInitialized) return false
        return name != initialWorkoutName || exercises.map { it.id } != initialExercises.map { it.id }
    }

    private fun saveExerciseDraft(exercises: List<Exercise>) {
        savedStateHandle[KEY_DRAFT_EXERCISE_IDS] = ArrayList(exercises.map { it.id })
        savedStateHandle[KEY_DRAFT_EXERCISES_EMPTY] = exercises.isEmpty()
    }

    private fun clearDraftKeys() {
        savedStateHandle.remove<String>(KEY_DRAFT_NAME)
        savedStateHandle.remove<ArrayList<String>>(KEY_DRAFT_EXERCISE_IDS)
        savedStateHandle.remove<List<String>>(KEY_DRAFT_EXERCISE_IDS)
        savedStateHandle.remove<Boolean>(KEY_DRAFT_EXERCISES_EMPTY)
    }

    private suspend fun rehydrateExercises(exerciseIds: List<String>): List<Exercise> {
        if (exerciseIds.isEmpty()) return emptyList()
        val exercisesMap = mutableMapOf<String, Exercise>()
        for (id in exerciseIds) {
            if (!exercisesMap.containsKey(id)) {
                val result = exerciseRepository.observeExercise(id).firstOrNull()
                if (result is DataResult.Success && result.data != null) {
                    exercisesMap[id] = result.data
                }
            }
        }
        return exerciseIds.mapNotNull { exercisesMap[it] }
    }

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
            initialWorkoutName = ""
            initialExercises = emptyList()
            isInitialized = true
            val draftName = savedStateHandle.get<String>(KEY_DRAFT_NAME) ?: ""
            val hasDraftExercises = savedStateHandle.contains(KEY_DRAFT_EXERCISE_IDS) ||
                savedStateHandle.contains(KEY_DRAFT_EXERCISES_EMPTY)
            val isDraftEmpty = savedStateHandle.get<Boolean>(KEY_DRAFT_EXERCISES_EMPTY) == true
            val draftIds = savedStateHandle.get<ArrayList<String>>(KEY_DRAFT_EXERCISE_IDS)?.toList()
                ?: savedStateHandle.get<List<String>>(KEY_DRAFT_EXERCISE_IDS)
                ?: emptyList()

            if (hasDraftExercises && !isDraftEmpty && draftIds.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        isMissing = false,
                        workoutId = null,
                        workoutName = draftName,
                        exercises = emptyList(),
                        hasUnsavedChanges = checkHasChanges(draftName, emptyList())
                    )
                }
                viewModelScope.launch {
                    val rehydrated = rehydrateExercises(draftIds)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            exercises = rehydrated,
                            hasUnsavedChanges = checkHasChanges(draftName, rehydrated)
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isMissing = false,
                        workoutId = null,
                        workoutName = draftName,
                        exercises = emptyList(),
                        hasUnsavedChanges = checkHasChanges(draftName, emptyList())
                    )
                }
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
                                    if (!isInitialized) {
                                        initialWorkoutName = workout.name
                                        initialExercises = workout.exercises
                                        isInitialized = true
                                        val draftName = savedStateHandle.get<String>(KEY_DRAFT_NAME) ?: workout.name

                                        val hasDraftExercises = savedStateHandle.contains(KEY_DRAFT_EXERCISE_IDS) ||
                                            savedStateHandle.contains(KEY_DRAFT_EXERCISES_EMPTY)
                                        val isDraftEmpty = savedStateHandle.get<Boolean>(KEY_DRAFT_EXERCISES_EMPTY) == true
                                        val draftIds = savedStateHandle.get<ArrayList<String>>(KEY_DRAFT_EXERCISE_IDS)?.toList()
                                            ?: savedStateHandle.get<List<String>>(KEY_DRAFT_EXERCISE_IDS)
                                            ?: emptyList()

                                        if (hasDraftExercises) {
                                            if (isDraftEmpty || draftIds.isEmpty()) {
                                                _uiState.update {
                                                    it.copy(
                                                        isLoading = false,
                                                        isMissing = false,
                                                        workoutId = workout.id,
                                                        workoutName = draftName,
                                                        exercises = emptyList(),
                                                        errorMessage = null,
                                                        hasUnsavedChanges = checkHasChanges(draftName, emptyList())
                                                    )
                                                }
                                            } else {
                                                val existingMap = workout.exercises.associateBy { it.id }.toMutableMap()
                                                val missingIds = draftIds.filter { !existingMap.containsKey(it) }
                                                val finalExercises = if (missingIds.isNotEmpty()) {
                                                    rehydrateExercises(draftIds)
                                                } else {
                                                    draftIds.mapNotNull { existingMap[it] }
                                                }
                                                _uiState.update {
                                                    it.copy(
                                                        isLoading = false,
                                                        isMissing = false,
                                                        workoutId = workout.id,
                                                        workoutName = draftName,
                                                        exercises = finalExercises,
                                                        errorMessage = null,
                                                        hasUnsavedChanges = checkHasChanges(draftName, finalExercises)
                                                    )
                                                }
                                            }
                                        } else {
                                            _uiState.update {
                                                it.copy(
                                                    isLoading = false,
                                                    isMissing = false,
                                                    workoutId = workout.id,
                                                    workoutName = draftName,
                                                    exercises = workout.exercises,
                                                    errorMessage = null,
                                                    hasUnsavedChanges = checkHasChanges(draftName, workout.exercises)
                                                )
                                            }
                                        }
                                    } else {
                                        _uiState.update { current ->
                                            current.copy(
                                                isLoading = false,
                                                isMissing = false,
                                                workoutId = workout.id,
                                                errorMessage = null
                                            )
                                        }
                                    }
                                    originalCreatedAt = workout.createdAt
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
            pickerQueryFlow.debounce { query ->
                if (query.isBlank()) 0L else 200L
            }.flatMapLatest { query ->
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
        savedStateHandle[KEY_DRAFT_NAME] = name
        _uiState.update {
            it.copy(
                workoutName = name,
                nameErrorRes = null,
                hasUnsavedChanges = checkHasChanges(name, it.exercises)
            )
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
            val updated = currentList + exercise
            saveExerciseDraft(updated)
            _uiState.update {
                it.copy(
                    exercises = updated,
                    exerciseErrorRes = null,
                    isPickerOpen = false,
                    hasUnsavedChanges = checkHasChanges(it.workoutName, updated)
                )
            }
        }
    }

    fun onRemoveExercise(index: Int) {
        val currentList = _uiState.value.exercises
        if (index in currentList.indices) {
            val updated = currentList.toMutableList().apply { removeAt(index) }
            saveExerciseDraft(updated)
            _uiState.update {
                it.copy(
                    exercises = updated,
                    hasUnsavedChanges = checkHasChanges(it.workoutName, updated)
                )
            }
        }
    }

    fun onMoveExerciseUp(index: Int) {
        val currentList = _uiState.value.exercises.toMutableList()
        if (index > 0 && index < currentList.size) {
            val item = currentList.removeAt(index)
            currentList.add(index - 1, item)
            saveExerciseDraft(currentList)
            _uiState.update {
                it.copy(
                    exercises = currentList,
                    hasUnsavedChanges = checkHasChanges(it.workoutName, currentList)
                )
            }
        }
    }

    fun onMoveExerciseDown(index: Int) {
        val currentList = _uiState.value.exercises.toMutableList()
        if (index >= 0 && index < currentList.size - 1) {
            val item = currentList.removeAt(index)
            currentList.add(index + 1, item)
            saveExerciseDraft(currentList)
            _uiState.update {
                it.copy(
                    exercises = currentList,
                    hasUnsavedChanges = checkHasChanges(it.workoutName, currentList)
                )
            }
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
            try {
                val workout = Workout(
                    id = currentState.workoutId ?: 0L,
                    name = currentState.workoutName,
                    createdAt = originalCreatedAt,
                    updatedAt = 0L,
                    exercises = currentState.exercises
                )
                when (val result = saveWorkoutUseCase(workout)) {
                    is SaveWorkoutResult.Success -> {
                        clearDraftKeys()
                        _uiState.update { it.copy(hasUnsavedChanges = false) }
                        _events.send(WorkoutEditorEvent.NavigateBack(result.workoutId))
                    }
                    is SaveWorkoutResult.InvalidName -> {
                        val resId = when (result.reason) {
                            NameResult.Blank -> R.string.error_workout_name_blank
                            NameResult.TooLong -> R.string.error_workout_name_too_long
                            is NameResult.Valid -> null
                        }
                        _uiState.update { it.copy(nameErrorRes = resId) }
                    }
                    is SaveWorkoutResult.InvalidExercises -> {
                        val resId = when (result.reason) {
                            ExerciseListResult.Empty -> R.string.error_workout_exercises_empty
                            is ExerciseListResult.Duplicate -> R.string.error_workout_exercise_duplicate
                            ExerciseListResult.Valid -> null
                        }
                        _uiState.update { it.copy(exerciseErrorRes = resId) }
                    }
                    is SaveWorkoutResult.RepositoryError -> {
                        val errorText = result.error.toUiText()
                        _uiState.update {
                            it.copy(errorMessage = errorText)
                        }
                        _events.send(WorkoutEditorEvent.ShowSnackbar(errorText))
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: java.lang.Error) {
                throw e
            } finally {
                _uiState.update { it.copy(isSaving = false) }
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
            try {
                val workout = Workout(
                    id = currentState.workoutId ?: 0L,
                    name = currentState.workoutName,
                    createdAt = originalCreatedAt,
                    updatedAt = 0L,
                    exercises = currentState.exercises
                )
                when (val saveResult = saveWorkoutUseCase(workout)) {
                    is SaveWorkoutResult.Success -> {
                        clearDraftKeys()
                        _uiState.update { it.copy(hasUnsavedChanges = false) }
                        val workoutId = saveResult.workoutId
                        when (val startResult = startWorkoutUseCase(workoutId)) {
                            is StartWorkoutResult.Success -> {
                                _events.send(WorkoutEditorEvent.NavigateToActiveWorkout(startResult.sessionId))
                            }
                            is StartWorkoutResult.ActiveSessionExists -> {
                                _events.send(WorkoutEditorEvent.NavigateToActiveWorkout(startResult.sessionId))
                            }
                            StartWorkoutResult.WorkoutNotFound -> {
                                _uiState.update {
                                    it.copy(errorMessage = UiText.StringResource(R.string.error_workout_not_found))
                                }
                            }
                            StartWorkoutResult.EmptyWorkout -> {
                                _uiState.update {
                                    it.copy(exerciseErrorRes = R.string.error_workout_exercises_empty)
                                }
                            }
                            is StartWorkoutResult.Failure -> {
                                val errorText = startResult.error.toUiText()
                                _uiState.update {
                                    it.copy(errorMessage = errorText)
                                }
                                _events.send(WorkoutEditorEvent.ShowSnackbar(errorText))
                            }
                        }
                    }
                    is SaveWorkoutResult.InvalidName -> {
                        val resId = when (saveResult.reason) {
                            NameResult.Blank -> R.string.error_workout_name_blank
                            NameResult.TooLong -> R.string.error_workout_name_too_long
                            is NameResult.Valid -> null
                        }
                        _uiState.update { it.copy(nameErrorRes = resId) }
                    }
                    is SaveWorkoutResult.InvalidExercises -> {
                        val resId = when (saveResult.reason) {
                            ExerciseListResult.Empty -> R.string.error_workout_exercises_empty
                            is ExerciseListResult.Duplicate -> R.string.error_workout_exercise_duplicate
                            ExerciseListResult.Valid -> null
                        }
                        _uiState.update { it.copy(exerciseErrorRes = resId) }
                    }
                    is SaveWorkoutResult.RepositoryError -> {
                        val errorText = saveResult.error.toUiText()
                        _uiState.update {
                            it.copy(errorMessage = errorText)
                        }
                        _events.send(WorkoutEditorEvent.ShowSnackbar(errorText))
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: java.lang.Error) {
                throw e
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
