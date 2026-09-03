package com.haitranduc.fittrack.domain.model

data class WorkoutSession(
    val id: Long,
    val workoutId: Long?,
    val workoutNameSnapshot: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val durationSeconds: Long?,
    val sets: List<SetLog>
)
