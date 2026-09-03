package com.haitranduc.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index(value = ["sessionId", "exerciseId", "setNumber"], unique = true)
    ]
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseNameSnapshot: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val completedAt: Long
)
