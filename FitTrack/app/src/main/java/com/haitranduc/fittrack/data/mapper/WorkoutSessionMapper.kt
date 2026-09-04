package com.haitranduc.fittrack.data.mapper

import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity
import com.haitranduc.fittrack.data.local.relation.WorkoutSessionWithSets
import com.haitranduc.fittrack.domain.model.WorkoutSession

fun WorkoutSessionWithSets.toDomain(): WorkoutSession {
    return WorkoutSession(
        id = session.id,
        workoutId = session.workoutId,
        workoutNameSnapshot = session.workoutNameSnapshot,
        startedAt = session.startedAt,
        finishedAt = session.finishedAt,
        durationSeconds = session.durationSeconds,
        sets = sets.map { it.toDomain() }
    )
}

fun WorkoutSession.toEntity(): WorkoutSessionEntity {
    return WorkoutSessionEntity(
        id = id,
        workoutId = workoutId,
        workoutNameSnapshot = workoutNameSnapshot,
        startedAt = startedAt,
        finishedAt = finishedAt,
        durationSeconds = durationSeconds
    )
}
