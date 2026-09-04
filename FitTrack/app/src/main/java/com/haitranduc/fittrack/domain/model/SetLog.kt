package com.haitranduc.fittrack.domain.model

data class SetLog(
    val id: Long,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseNameSnapshot: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val completedAt: Long
)
