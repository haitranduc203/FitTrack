package com.haitranduc.fittrack.data.local.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedExerciseDto(
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val muscleGroup: String,
    val secondaryMuscles: List<String>,
    val instructions: List<String>
) {
    fun isValid(): Boolean {
        if (id.isBlank() || name.isBlank() || bodyPart.isBlank() ||
            equipment.isBlank() || target.isBlank() || muscleGroup.isBlank()
        ) {
            return false
        }
        if (instructions.isEmpty() || instructions.any { it.isBlank() }) {
            return false
        }
        if (secondaryMuscles.any { it.isBlank() }) {
            return false
        }
        return true
    }
}
