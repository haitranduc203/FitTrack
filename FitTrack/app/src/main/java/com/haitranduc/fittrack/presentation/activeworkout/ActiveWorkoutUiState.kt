package com.haitranduc.fittrack.presentation.activeworkout

import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.WorkoutSession

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val isMissing: Boolean = false,
    val errorMessage: String? = null,
    val session: WorkoutSession? = null,
    val exercises: List<Exercise> = emptyList(),
    val completedSets: List<SetLog> = emptyList(),
    val inputReps: Map<String, String> = emptyMap(),
    val inputWeight: Map<String, String> = emptyMap(),
    val inputErrors: Map<String, String> = emptyMap(),
    val isCompletingSet: Boolean = false,
    val isFinishing: Boolean = false,
    val finishError: String? = null,
    val finishedSessionId: Long? = null,
    val elapsedTimeSeconds: Long = 0L
)
