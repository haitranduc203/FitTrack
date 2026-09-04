package com.haitranduc.fittrack.presentation.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import com.haitranduc.fittrack.presentation.util.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseIdFlow = MutableStateFlow<String?>(savedStateHandle.get<String>("exerciseId"))
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        exerciseIdFlow,
        retryTrigger
    ) { id, _ -> id }
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(ExerciseDetailUiState(isLoading = false, isMissing = true))
            } else {
                flow {
                    emit(ExerciseDetailUiState(isLoading = true))
                    exerciseRepository.observeExercise(id).collect { result ->
                        when (result) {
                            is DataResult.Success -> {
                                if (result.data == null) {
                                    emit(ExerciseDetailUiState(isLoading = false, isMissing = true))
                                } else {
                                    emit(ExerciseDetailUiState(isLoading = false, exercise = result.data))
                                }
                            }
                            is DataResult.Failure -> {
                                emit(ExerciseDetailUiState(isLoading = false, errorMessage = result.error.toUiText()))
                            }
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
        retryTrigger.value++
    }
}
