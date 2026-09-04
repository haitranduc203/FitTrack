package com.haitranduc.fittrack.presentation.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import com.haitranduc.fittrack.domain.repository.FavoriteExerciseRepository
import com.haitranduc.fittrack.presentation.util.UiText
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val favoriteExerciseRepository: FavoriteExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseIdFlow = MutableStateFlow<String?>(savedStateHandle.get<String>("exerciseId"))
    private val isTogglingFavorite = MutableStateFlow(false)
    private val favoriteErrorMessage = MutableStateFlow<UiText?>(null)
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

                    combine(
                        exerciseRepository.observeExercise(id),
                        favoriteExerciseRepository.observeFavoriteIds(),
                        isTogglingFavorite,
                        favoriteErrorMessage
                    ) { exerciseResult, favoriteIdsResult, toggling, favError ->
                        when (exerciseResult) {
                            is DataResult.Success -> {
                                if (exerciseResult.data == null) {
                                    ExerciseDetailUiState(isLoading = false, isMissing = true)
                                } else {
                                    val isFav = when (favoriteIdsResult) {
                                        is DataResult.Success -> favoriteIdsResult.data.contains(id)
                                        is DataResult.Failure -> false
                                    }
                                    ExerciseDetailUiState(
                                        isLoading = false,
                                        exercise = exerciseResult.data,
                                        isFavorite = isFav,
                                        isTogglingFavorite = toggling,
                                        favoriteErrorMessage = favError
                                    )
                                }
                            }
                            is DataResult.Failure -> {
                                ExerciseDetailUiState(
                                    isLoading = false,
                                    errorMessage = exerciseResult.error.toUiText(),
                                    isTogglingFavorite = toggling,
                                    favoriteErrorMessage = favError
                                )
                            }
                        }
                    }.collect { state ->
                        emit(state)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExerciseDetailUiState(isLoading = true)
        )

    fun onToggleFavorite() {
        val exerciseId = uiState.value.exercise?.id ?: return
        if (isTogglingFavorite.value) return

        isTogglingFavorite.value = true
        favoriteErrorMessage.value = null

        viewModelScope.launch {
            val isCurrentlyFav = uiState.value.isFavorite
            when (val res = favoriteExerciseRepository.setFavorite(exerciseId, !isCurrentlyFav)) {
                is DataResult.Success -> {
                    // Handled reactively via Flow
                }
                is DataResult.Failure -> {
                    favoriteErrorMessage.value = res.error.toUiText()
                }
            }
            isTogglingFavorite.value = false
        }
    }

    fun onClearFavoriteError() {
        favoriteErrorMessage.value = null
    }

    fun retry() {
        retryTrigger.value++
    }
}
