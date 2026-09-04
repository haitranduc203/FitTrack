package com.haitranduc.fittrack.presentation.activeworkout

import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.presentation.util.UiText

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val isMissing: Boolean = false,
    val errorMessage: UiText? = null,
    val session: WorkoutSession? = null,
    val exercises: List<Exercise> = emptyList(),
    val completedSets: List<SetLog> = emptyList(),
    val inputReps: Map<String, String> = emptyMap(),
    val inputWeight: Map<String, String> = emptyMap(),
    val inputErrors: Map<String, UiText> = emptyMap(),
    val isCompletingSet: Boolean = false,
    val isFinishing: Boolean = false,
    val finishError: UiText? = null,
    val finishedSessionId: Long? = null,
    val elapsedTimeSeconds: Long = 0L,
    val restTimerEndsAtMillis: Long? = null,
    val restTimerRemainingSeconds: Long = 0L
)
