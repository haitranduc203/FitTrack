package com.haitranduc.fittrack.data.local.relation

import androidx.room.Embedded
import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity

data class WorkoutSessionWithSets(
    @Embedded
    val session: WorkoutSessionEntity,
    val sets: List<SetLogEntity>
)
