package com.haitranduc.fittrack.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    private val filtersFlow = combine(
        searchQuery,
        selectedBodyPart,
        selectedEquipment
    ) { query, bodyPart, equipment ->
        Triple(query, bodyPart, equipment)
    }

    val uiState: StateFlow<ExerciseListUiState> = filtersFlow
        .flatMapLatest { (query, bodyPart, equipment) ->
            combine(
                exerciseRepository.observeExercises(
                    query = query.trim(),
                    bodyPart = bodyPart,
                    equipment = equipment
                )
            ) { results ->
                val result = results[0]
                when (result) {
                    is DataResult.Success -> {
                        ExerciseListUiState(
                            isLoading = false,
                            exercises = result.data,
                            searchQuery = query,
                            selectedBodyPart = bodyPart,
                            selectedEquipment = equipment,
                            errorMessage = null
                        )
                    }
                    is DataResult.Failure -> {
                        ExerciseListUiState(
                            isLoading = false,
                            exercises = emptyList(),
                            searchQuery = query,
                            selectedBodyPart = bodyPart,
                            selectedEquipment = equipment,
                            errorMessage = result.error.toString()
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExerciseListUiState(isLoading = true)
        )

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
