package com.haitranduc.fittrack.presentation.history

import androidx.annotation.StringRes
import com.haitranduc.fittrack.R

data class HistorySessionMock(
    val id: String,
    @get:StringRes val workoutNameRes: Int,
    @get:StringRes val dateRes: Int,
    @get:StringRes val durationRes: Int,
    val completedSetsCount: Int,
    val exercises: List<HistoryExerciseMock>
)

data class HistoryExerciseMock(
    @get:StringRes val exerciseNameRes: Int,
    val sets: List<HistorySetMock>
)

data class HistorySetMock(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int
)

object HistoryMockData {
    val sessions = listOf(
        HistorySessionMock(
            id = "session_push_1",
            workoutNameRes = R.string.workout_template_push_day,
            dateRes = R.string.history_date_yesterday,
            durationRes = R.string.history_duration_48m,
            completedSetsCount = 5,
            exercises = listOf(
                HistoryExerciseMock(
                    exerciseNameRes = R.string.exercise_name_bench_press,
                    sets = listOf(
                        HistorySetMock(1, 50.0, 10),
                        HistorySetMock(2, 55.0, 8),
                        HistorySetMock(3, 60.0, 6)
                    )
                ),
                HistoryExerciseMock(
                    exerciseNameRes = R.string.exercise_name_overhead_press,
                    sets = listOf(
                        HistorySetMock(1, 30.0, 10),
                        HistorySetMock(2, 35.0, 8)
                    )
                )
            )
        ),
        HistorySessionMock(
            id = "session_pull_1",
            workoutNameRes = R.string.workout_template_pull_day,
            dateRes = R.string.history_date_3days,
            durationRes = R.string.history_duration_42m,
            completedSetsCount = 4,
            exercises = listOf(
                HistoryExerciseMock(
                    exerciseNameRes = R.string.exercise_name_deadlift,
                    sets = listOf(
                        HistorySetMock(1, 90.0, 5),
                        HistorySetMock(2, 100.0, 5)
                    )
                ),
                HistoryExerciseMock(
                    exerciseNameRes = R.string.exercise_name_pull_up,
                    sets = listOf(
                        HistorySetMock(1, 0.0, 8),
                        HistorySetMock(2, 0.0, 6)
                    )
                )
            )
        )
    )

    fun find(id: String): HistorySessionMock? = sessions.find { it.id == id }
}
