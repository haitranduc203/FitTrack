package com.haitranduc.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [
        Index("name"),
        Index("bodyPart"),
        Index("equipment")
    ]
)
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val muscleGroup: String,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val imagePath: String? = null,
    val gifPath: String? = null
)
