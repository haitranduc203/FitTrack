package com.haitranduc.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "workout_exercises",
    primaryKeys = ["workoutId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("workoutId"),
        Index("exerciseId"),
        Index(value = ["workoutId", "orderIndex"], unique = true)
    ]
)
data class WorkoutExerciseEntity(
    val workoutId: Long,
    val exerciseId: String,
    val orderIndex: Int
)
