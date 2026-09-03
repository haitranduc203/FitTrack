package com.haitranduc.fittrack.core.navigation

object FitTrackDestination {
    const val EXERCISES = "exercises"
    const val WORKOUTS = "workouts"
    const val HISTORY = "history"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val WORKOUT_EDITOR = "workout_editor"
    const val WORKOUT_EDITOR_WITH_ID = "workout_editor/{workoutId}"
    const val ACTIVE_WORKOUT = "active_workout"
    const val HISTORY_DETAIL = "history_detail/{sessionId}"

    fun exerciseDetailRoute(exerciseId: String): String = "exercise_detail/$exerciseId"
    fun workoutEditorRoute(workoutId: String): String = "workout_editor/$workoutId"
    fun historyDetailRoute(sessionId: String): String = "history_detail/$sessionId"
}
