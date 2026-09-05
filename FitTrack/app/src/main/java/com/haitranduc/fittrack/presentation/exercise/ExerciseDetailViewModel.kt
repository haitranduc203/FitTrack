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

    private var lastExercise: com.haitranduc.fittrack.domain.model.Exercise? = null

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        exerciseIdFlow,
        retryTrigger
    ) { id, _ -> id }
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(ExerciseDetailUiState(isLoading = false, isMissing = true))
            } else {
                flow {
                    emit(ExerciseDetailUiState(isLoading = lastExercise == null, exercise = lastExercise))

                    combine(
                        exerciseRepository.observeExercise(id),
                        favoriteExerciseRepository.observeFavoriteIds(),
                        isTogglingFavorite,
                        favoriteErrorMessage
                    ) { exerciseResult, favoriteIdsResult, toggling, favError ->
                        when (exerciseResult) {
                            is DataResult.Success -> {
                                if (exerciseResult.data == null) {
                                    lastExercise = null
                                    ExerciseDetailUiState(isLoading = false, isMissing = true)
                                } else {
                                    lastExercise = exerciseResult.data
                                    val isFav: Boolean?
                                    val favErrorToSurface: UiText?
                                    when (favoriteIdsResult) {
                                        is DataResult.Success -> {
                                            isFav = favoriteIdsResult.data.contains(id)
                                            favErrorToSurface = favError
                                        }
                                        is DataResult.Failure -> {
                                            isFav = null
                                            favErrorToSurface = favError ?: favoriteIdsResult.error.toUiText()
                                        }
                                    }
                                    ExerciseDetailUiState(
                                        isLoading = false,
                                        exercise = exerciseResult.data,
                                        isFavorite = isFav,
                                        isTogglingFavorite = toggling,
                                        favoriteErrorMessage = favErrorToSurface
                                    )
                                }
                            }
                            is DataResult.Failure -> {
                                ExerciseDetailUiState(
                                    isLoading = false,
                                    exercise = lastExercise,
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
        val isCurrentlyFav = uiState.value.isFavorite ?: return
        if (isTogglingFavorite.value) return

        isTogglingFavorite.value = true
        favoriteErrorMessage.value = null

        viewModelScope.launch {
            try {
                when (val res = favoriteExerciseRepository.setFavorite(exerciseId, !isCurrentlyFav)) {
                    is DataResult.Success -> {
                        // Handled reactively via Flow
                    }
                    is DataResult.Failure -> {
                        favoriteErrorMessage.value = res.error.toUiText()
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: java.lang.Error) {
                throw e
            } finally {
                isTogglingFavorite.value = false
            }
        }
    }

    fun onClearFavoriteError() {
        favoriteErrorMessage.value = null
    }

    fun retry() {
        favoriteErrorMessage.value = null
        retryTrigger.value++
    }
}
