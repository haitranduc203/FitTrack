package com.haitranduc.fittrack.presentation.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import com.haitranduc.fittrack.domain.repository.FavoriteExerciseRepository
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.presentation.util.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExerciseListEvent {
    data class ShowSnackbar(val message: UiText) : ExerciseListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val favoriteExerciseRepository: FavoriteExerciseRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    companion object {
        const val KEY_SEARCH_QUERY = "search_query"
        const val KEY_BODY_PART = "selected_body_part"
        const val KEY_EQUIPMENT = "selected_equipment"
        const val KEY_FAVORITES_ONLY = "is_favorites_only"
    }

    private val _events = Channel<ExerciseListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val searchQuery = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")
    private val selectedBodyPart = savedStateHandle.getStateFlow<String?>(KEY_BODY_PART, null)
    private val selectedEquipment = savedStateHandle.getStateFlow<String?>(KEY_EQUIPMENT, null)
    private val isFavoritesOnly = savedStateHandle.getStateFlow(KEY_FAVORITES_ONLY, false)
    private val pendingFavoriteIds = MutableStateFlow<Set<String>>(emptySet())
    private val favoriteErrorMessage = MutableStateFlow<UiText?>(null)
    private val retryTrigger = MutableStateFlow(0)

    private val filtersFlow = combine(
        searchQuery,
        selectedBodyPart,
        selectedEquipment,
        isFavoritesOnly
    ) { query, bodyPart, equipment, favOnly ->
        Filters(query, bodyPart, equipment, favOnly)
    }

    private data class Filters(
        val query: String,
        val bodyPart: String?,
        val equipment: String?,
        val isFavoritesOnly: Boolean
    )

    private var lastLoadedExercises: List<Exercise> = emptyList()

    val uiState: StateFlow<ExerciseListUiState> = combine(
        filtersFlow,
        retryTrigger
    ) { filters, _ -> filters }
        .flatMapLatest { filters ->
            flow {
                emit(
                    ExerciseListUiState(
                        isLoading = lastLoadedExercises.isEmpty(),
                        exercises = lastLoadedExercises,
                        searchQuery = filters.query,
                        selectedBodyPart = filters.bodyPart,
                        selectedEquipment = filters.equipment,
                        isFavoritesOnly = filters.isFavoritesOnly,
                        errorMessage = null
                    )
                )

                combine(
                    exerciseRepository.observeExercises(
                        query = filters.query.trim(),
                        bodyPart = filters.bodyPart,
                        equipment = filters.equipment
                    ),
                    favoriteExerciseRepository.observeFavoriteIds(),
                    pendingFavoriteIds,
                    favoriteErrorMessage
                ) { exercisesResult, favoriteIdsResult, pending, favError ->
                    when (exercisesResult) {
                        is DataResult.Success -> {
                            val favoriteIds = when (favoriteIdsResult) {
                                is DataResult.Success -> favoriteIdsResult.data
                                is DataResult.Failure -> emptySet()
                            }
                            val rawExercises = exercisesResult.data
                            val displayedExercises = if (filters.isFavoritesOnly) {
                                rawExercises.filter { it.id in favoriteIds }
                            } else {
                                rawExercises
                            }
                            lastLoadedExercises = displayedExercises
                            val errorMsg = if (favoriteIdsResult is DataResult.Failure && favError == null) {
                                favoriteIdsResult.error.toUiText()
                            } else {
                                favError
                            }
                            ExerciseListUiState(
                                isLoading = false,
                                exercises = displayedExercises,
                                searchQuery = filters.query,
                                selectedBodyPart = filters.bodyPart,
                                selectedEquipment = filters.equipment,
                                errorMessage = null,
                                favoriteExerciseIds = favoriteIds,
                                isFavoritesOnly = filters.isFavoritesOnly,
                                pendingFavoriteIds = pending,
                                favoriteErrorMessage = errorMsg
                            )
                        }
                        is DataResult.Failure -> {
                            ExerciseListUiState(
                                isLoading = false,
                                exercises = lastLoadedExercises,
                                searchQuery = filters.query,
                                selectedBodyPart = filters.bodyPart,
                                selectedEquipment = filters.equipment,
                                errorMessage = exercisesResult.error.toUiText(),
                                isFavoritesOnly = filters.isFavoritesOnly,
                                pendingFavoriteIds = pending,
                                favoriteErrorMessage = favError
                            )
                        }
                    }
                }.collect { state ->
                    emit(state)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExerciseListUiState(isLoading = true)
        )

    fun onRetry() {
        retryTrigger.value++
    }

    fun onSearchQueryChanged(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun onBodyPartSelected(bodyPart: String?) {
        savedStateHandle[KEY_BODY_PART] = bodyPart
    }

    fun onEquipmentSelected(equipment: String?) {
        savedStateHandle[KEY_EQUIPMENT] = equipment
    }

    fun onFavoritesFilterToggled(enabled: Boolean) {
        savedStateHandle[KEY_FAVORITES_ONLY] = enabled
    }

    fun onToggleFavorite(exerciseId: String) {
        if (exerciseId in pendingFavoriteIds.value) return
        pendingFavoriteIds.update { it + exerciseId }
        favoriteErrorMessage.value = null

        viewModelScope.launch {
            try {
                val isCurrentlyFavorite = uiState.value.favoriteExerciseIds.contains(exerciseId)
                val newTarget = !isCurrentlyFavorite
                when (val res = favoriteExerciseRepository.setFavorite(exerciseId, newTarget)) {
                    is DataResult.Success -> {
                        _events.send(
                            ExerciseListEvent.ShowSnackbar(
                                UiText.StringResource(
                                    if (newTarget) R.string.msg_favorite_added else R.string.msg_favorite_removed
                                )
                            )
                        )
                    }
                    is DataResult.Failure -> {
                        val err = res.error.toUiText()
                        favoriteErrorMessage.value = err
                        _events.send(ExerciseListEvent.ShowSnackbar(err))
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: java.lang.Error) {
                throw e
            } finally {
                pendingFavoriteIds.update { it - exerciseId }
            }
        }
    }

    fun onClearFavoriteError() {
        favoriteErrorMessage.value = null
    }
}
