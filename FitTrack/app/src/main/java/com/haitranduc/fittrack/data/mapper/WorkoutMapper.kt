package com.haitranduc.fittrack.data.mapper

import com.haitranduc.fittrack.data.local.entity.WorkoutEntity
import com.haitranduc.fittrack.data.local.relation.WorkoutWithExercises
import com.haitranduc.fittrack.domain.model.Workout

fun WorkoutWithExercises.toDomain(): Workout {
    return Workout(
        id = workout.id,
        name = workout.name,
        createdAt = workout.createdAt,
        updatedAt = workout.updatedAt,
        exercises = exercises.map { it.exercise.toDomain() }
    )
}

fun Workout.toEntity(): WorkoutEntity {
    return WorkoutEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
