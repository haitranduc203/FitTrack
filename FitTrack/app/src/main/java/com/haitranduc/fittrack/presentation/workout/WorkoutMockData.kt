package com.haitranduc.fittrack.presentation.workout

import androidx.annotation.StringRes
import com.haitranduc.fittrack.R

data class WorkoutTemplateMock(
    val id: String,
    @get:StringRes val nameRes: Int,
    val exercises: List<WorkoutExerciseMock>
)

data class WorkoutExerciseMock(
    val exerciseId: String,
    @get:StringRes val exerciseNameRes: Int,
    val defaultSets: Int = 3,
    @get:StringRes val targetRepsRes: Int = R.string.reps_range_8_12
)

object WorkoutMockData {
    val templates = listOf(
        WorkoutTemplateMock(
            id = "push_day",
            nameRes = R.string.workout_template_push_day,
            exercises = listOf(
                WorkoutExerciseMock("bench_press", R.string.exercise_name_bench_press, 4, R.string.reps_range_8_10),
                WorkoutExerciseMock("overhead_press", R.string.exercise_name_overhead_press, 3, R.string.reps_range_10_12)
            )
        ),
        WorkoutTemplateMock(
            id = "pull_day",
            nameRes = R.string.workout_template_pull_day,
            exercises = listOf(
                WorkoutExerciseMock("deadlift", R.string.exercise_name_deadlift, 3, R.string.reps_range_5),
                WorkoutExerciseMock("pull_up", R.string.exercise_name_pull_up, 3, R.string.reps_range_8_10)
            )
        ),
        WorkoutTemplateMock(
            id = "leg_day",
            nameRes = R.string.workout_template_leg_day,
            exercises = listOf(
                WorkoutExerciseMock("barbell_squat", R.string.exercise_name_squat, 4, R.string.reps_range_8_10)
            )
        )
    )

    fun find(id: String): WorkoutTemplateMock? = templates.find { it.id == id }
}
