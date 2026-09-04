package com.haitranduc.fittrack.domain.model

data class Workout(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val exercises: List<Exercise>
)
