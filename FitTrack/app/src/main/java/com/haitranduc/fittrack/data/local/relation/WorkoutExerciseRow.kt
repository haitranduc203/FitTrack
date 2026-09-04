package com.haitranduc.fittrack.data.local.relation

import androidx.room.Embedded
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity

data class WorkoutExerciseRow(
    @Embedded
    val exercise: ExerciseEntity,
    val orderIndex: Int
)
