package com.haitranduc.fittrack.data.local.relation

import androidx.room.Embedded
import com.haitranduc.fittrack.data.local.entity.WorkoutEntity

data class WorkoutWithExercises(
    @Embedded
    val workout: WorkoutEntity,
    val exercises: List<WorkoutExerciseRow>
)
