package com.haitranduc.fittrack.presentation.exercise

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
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedBodyPart = MutableStateFlow<String?>(null)
    private val selectedEquipment = MutableStateFlow<String?>(null)
    private val retryTrigger = MutableStateFlow(0)

    private val filtersFlow = combine(
        searchQuery,
        selectedBodyPart,
        selectedEquipment
    ) { query, bodyPart, equipment ->
        Triple(query, bodyPart, equipment)
    }

    val uiState: StateFlow<ExerciseListUiState> = combine(
        filtersFlow,
        retryTrigger
    ) { filters, _ -> filters }
        .flatMapLatest { (query, bodyPart, equipment) ->
            flow {
                emit(
                    ExerciseListUiState(
                        isLoading = true,
                        exercises = emptyList(),
                        searchQuery = query,
                        selectedBodyPart = bodyPart,
                        selectedEquipment = equipment,
                        errorMessage = null
                    )
                )
                exerciseRepository.observeExercises(
                    query = query.trim(),
                    bodyPart = bodyPart,
                    equipment = equipment
                ).collect { result ->
                    when (result) {
                        is DataResult.Success -> {
                            emit(
                                ExerciseListUiState(
                                    isLoading = false,
                                    exercises = result.data,
                                    searchQuery = query,
                                    selectedBodyPart = bodyPart,
                                    selectedEquipment = equipment,
                                    errorMessage = null
                                )
                            )
                        }
                        is DataResult.Failure -> {
                            emit(
                                ExerciseListUiState(
                                    isLoading = false,
                                    exercises = emptyList(),
                                    searchQuery = query,
                                    selectedBodyPart = bodyPart,
                                    selectedEquipment = equipment,
                                    errorMessage = result.error.toUiText()
                                )
                            )
                        }
                    }
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
}
