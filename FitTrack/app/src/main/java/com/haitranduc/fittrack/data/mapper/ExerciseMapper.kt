package com.haitranduc.fittrack.data.mapper

import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.domain.model.Exercise

fun ExerciseEntity.toDomain(): Exercise {
    return Exercise(
        id = id,
        name = name,
        bodyPart = bodyPart,
        equipment = equipment,
        target = target,
        muscleGroup = muscleGroup,
        secondaryMuscles = secondaryMuscles,
        instructions = instructions
    )
}
