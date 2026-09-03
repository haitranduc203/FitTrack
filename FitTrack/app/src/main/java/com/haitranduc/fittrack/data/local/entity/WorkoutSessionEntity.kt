package com.haitranduc.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("workoutId"),
        Index("finishedAt")
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val workoutId: Long?,
    val workoutNameSnapshot: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val durationSeconds: Long?
)
