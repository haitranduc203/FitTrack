package com.haitranduc.fittrack.presentation.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseIdFlow = MutableStateFlow<String?>(savedStateHandle.get<String>("exerciseId"))

    val uiState: StateFlow<ExerciseDetailUiState> = exerciseIdFlow
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(ExerciseDetailUiState(isLoading = false, isMissing = true))
            } else {
                exerciseRepository.observeExercise(id).map { result ->
                    when (result) {
                        is DataResult.Success -> {
                            if (result.data == null) {
                                ExerciseDetailUiState(isLoading = false, isMissing = true)
                            } else {
                                ExerciseDetailUiState(isLoading = false, exercise = result.data)
                            }
                        }
                        is DataResult.Failure -> {
                            ExerciseDetailUiState(isLoading = false, errorMessage = result.error.toString())
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExerciseDetailUiState(isLoading = true)
        )

    fun retry() {
        val current = exerciseIdFlow.value
        exerciseIdFlow.value = null
        exerciseIdFlow.value = current
    }
}
