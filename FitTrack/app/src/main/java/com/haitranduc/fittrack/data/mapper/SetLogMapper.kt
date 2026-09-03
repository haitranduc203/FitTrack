package com.haitranduc.fittrack.data.mapper

import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import com.haitranduc.fittrack.domain.model.SetLog

fun SetLogEntity.toDomain(): SetLog {
    return SetLog(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        exerciseNameSnapshot = exerciseNameSnapshot,
        setNumber = setNumber,
        reps = reps,
        weightKg = weightKg,
        completedAt = completedAt
    )
}

fun SetLog.toEntity(): SetLogEntity {
    return SetLogEntity(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        exerciseNameSnapshot = exerciseNameSnapshot,
        setNumber = setNumber,
        reps = reps,
        weightKg = weightKg,
        completedAt = completedAt
    )
}
