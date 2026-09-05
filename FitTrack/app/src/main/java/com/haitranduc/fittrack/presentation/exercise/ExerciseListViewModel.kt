package com.haitranduc.fittrack.presentation.exercise

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val favoriteExerciseRepository: FavoriteExerciseRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedBodyPart = MutableStateFlow<String?>(null)
    private val selectedEquipment = MutableStateFlow<String?>(null)
    private val isFavoritesOnly = MutableStateFlow(false)
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

    val uiState: StateFlow<ExerciseListUiState> = combine(
        filtersFlow,
        retryTrigger
    ) { filters, _ -> filters }
        .flatMapLatest { filters ->
            flow {
                emit(
                    ExerciseListUiState(
                        isLoading = true,
                        exercises = emptyList(),
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
                                exercises = emptyList(),
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
        searchQuery.value = query
    }

    fun onBodyPartSelected(bodyPart: String?) {
        selectedBodyPart.value = bodyPart
    }

    fun onEquipmentSelected(equipment: String?) {
        selectedEquipment.value = equipment
    }

    fun onFavoritesFilterToggled(enabled: Boolean) {
        isFavoritesOnly.value = enabled
    }

    fun onToggleFavorite(exerciseId: String) {
        if (exerciseId in pendingFavoriteIds.value) return
        pendingFavoriteIds.update { it + exerciseId }
        favoriteErrorMessage.value = null

        viewModelScope.launch {
            val isCurrentlyFavorite = uiState.value.favoriteExerciseIds.contains(exerciseId)
            when (val res = favoriteExerciseRepository.setFavorite(exerciseId, !isCurrentlyFavorite)) {
                is DataResult.Success -> {
                    // Success handled reactively via Flow
                }
                is DataResult.Failure -> {
                    favoriteErrorMessage.value = res.error.toUiText()
                }
            }
            pendingFavoriteIds.update { it - exerciseId }
        }
    }

    fun onClearFavoriteError() {
        favoriteErrorMessage.value = null
    }
}
