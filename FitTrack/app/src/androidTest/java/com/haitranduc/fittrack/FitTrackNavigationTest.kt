package com.haitranduc.fittrack

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.designsystem.theme.FitTrackTheme
import com.haitranduc.fittrack.core.navigation.FitTrackNavHost
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FitTrackNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun test_bottomNavigationDestinations() {
        composeTestRule.setContent {
            FitTrackTheme {
                FitTrackNavHost()
            }
        }

        val navExercises = context.getString(R.string.nav_exercises)
        val navWorkouts = context.getString(R.string.nav_workouts)
        val navHistory = context.getString(R.string.nav_history)
        val titleExercises = context.getString(R.string.title_exercises)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val titleHistory = context.getString(R.string.title_history)

        // 1. Initially on Exercises destination
        composeTestRule.onAllNodesWithText(titleExercises).onFirst().assertIsDisplayed()

        // 2. Navigate to Workouts via bottom bar
        composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()

        // 3. Navigate to History via bottom bar
        composeTestRule.onAllNodesWithText(navHistory).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleHistory).onFirst().assertIsDisplayed()

        // 4. Return to Exercises via bottom bar
        composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleExercises).onFirst().assertIsDisplayed()
    }

    @Test
    fun test_exerciseList_to_exerciseDetail_andBack() {
        composeTestRule.setContent {
            FitTrackTheme {
                FitTrackNavHost()
            }
        }

        val titleExercises = context.getString(R.string.title_exercises)
        val benchPressName = context.getString(R.string.exercise_name_bench_press)
        val labelInstructions = context.getString(R.string.label_instructions)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)

        // Verify on Exercises list
        composeTestRule.onAllNodesWithText(titleExercises).onFirst().assertIsDisplayed()

        // Click Barbell Bench Press card
        composeTestRule.onAllNodesWithText(benchPressName).onFirst().performClick()

        // Verify on Detail screen
        composeTestRule.onNodeWithText(labelInstructions).assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()

        // Verify back on Exercises list
        composeTestRule.onAllNodesWithText(titleExercises).onFirst().assertIsDisplayed()
    }

    @Test
    fun test_workoutList_to_workoutEditor_to_activeWorkout_andFinish() {
        composeTestRule.setContent {
            FitTrackTheme {
                FitTrackNavHost()
            }
        }

        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val pushDayName = context.getString(R.string.workout_template_push_day)
        val btnStartWorkout = context.getString(R.string.btn_start_workout)
        val titleActiveWorkout = context.getString(R.string.title_active_workout)
        val btnFinishWorkout = context.getString(R.string.btn_finish_workout)

        // Navigate to Workouts tab via bottom bar
        composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()

        // Click on Push Day template card
        composeTestRule.onAllNodesWithText(pushDayName).onFirst().performClick()

        // Verify on Workout Editor screen
        composeTestRule.onNodeWithText(btnStartWorkout).assertIsDisplayed()

        // Click Start Workout
        composeTestRule.onNodeWithText(btnStartWorkout).performClick()

        // Verify on Active Workout screen
        composeTestRule.onNodeWithText(titleActiveWorkout).assertIsDisplayed()
        composeTestRule.onNodeWithText(btnFinishWorkout).assertIsDisplayed()

        // Finish workout
        composeTestRule.onNodeWithText(btnFinishWorkout).performClick()

        // Verify back on Workouts list
        composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()
    }

    @Test
    fun test_historyList_to_historyDetail_andBack() {
        composeTestRule.setContent {
            FitTrackTheme {
                FitTrackNavHost()
            }
        }

        val navHistory = context.getString(R.string.nav_history)
        val titleHistory = context.getString(R.string.title_history)
        val pushDayName = context.getString(R.string.workout_template_push_day)
        val titleHistoryDetail = context.getString(R.string.title_history_detail)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)

        // Navigate to History tab via bottom bar
        composeTestRule.onAllNodesWithText(navHistory).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleHistory).onFirst().assertIsDisplayed()

        // Click on Push Day history item
        composeTestRule.onAllNodesWithText(pushDayName).onFirst().performClick()

        // Verify on History Detail screen
        composeTestRule.onNodeWithText(titleHistoryDetail).assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()

        // Verify back on History list
        composeTestRule.onAllNodesWithText(titleHistory).onFirst().assertIsDisplayed()
    }

    @Test
    fun test_searchNonMatchingQuery_showsEmptyState() {
        composeTestRule.setContent {
            FitTrackTheme {
                FitTrackNavHost()
            }
        }

        val emptyExercisesText = context.getString(R.string.empty_exercises)
        val benchPressName = context.getString(R.string.exercise_name_bench_press)

        // Bench press is visible initially
        composeTestRule.onAllNodesWithText(benchPressName).onFirst().assertIsDisplayed()

        // Enter non-matching search query
        composeTestRule.onNode(hasSetTextAction()).performTextInput("XYZNonExistentExercise123")

        // Verify empty state is displayed and bench press is gone
        composeTestRule.onNodeWithText(emptyExercisesText).assertIsDisplayed()
        composeTestRule.onNode(hasText(benchPressName)).assertDoesNotExist()
    }

    @Test
    fun test_bodyPartFilter_filtersCorrectExercises() {
        composeTestRule.setContent {
            FitTrackTheme {
                FitTrackNavHost()
            }
        }

        val filterChest = context.getString(R.string.filter_chest)
        val benchPressName = context.getString(R.string.exercise_name_bench_press)
        val deadliftName = context.getString(R.string.exercise_name_deadlift)

        // Both Bench Press (Chest) and Deadlift (Back) exist initially
        composeTestRule.onAllNodesWithText(benchPressName).onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText(deadliftName).onFirst().assertIsDisplayed()

        // Click Chest filter chip (which is selectable)
        composeTestRule.onNode(hasText(filterChest) and isSelectable()).performClick()

        // Bench press must still be displayed, while Deadlift must not exist
        composeTestRule.onAllNodesWithText(benchPressName).onFirst().assertIsDisplayed()
        composeTestRule.onNode(hasText(deadliftName)).assertDoesNotExist()
    }

    @Test
    fun test_equipmentFilter_filtersCorrectExercises() {
        composeTestRule.setContent {
            FitTrackTheme {
                FitTrackNavHost()
            }
        }

        val filterBodyweight = context.getString(R.string.filter_bodyweight)
        val pullUpName = context.getString(R.string.exercise_name_pull_up)
        val benchPressName = context.getString(R.string.exercise_name_bench_press)

        // Click Bodyweight filter chip (which is selectable)
        composeTestRule.onNode(hasText(filterBodyweight) and isSelectable()).performClick()

        // Pull Up (Bodyweight) must be displayed, Bench Press (Barbell) must not exist
        composeTestRule.onAllNodesWithText(pullUpName).onFirst().assertIsDisplayed()
        composeTestRule.onNode(hasText(benchPressName)).assertDoesNotExist()
    }

    @Test
    fun test_combinedFilterAndSearch_doesNotCrash() {
        composeTestRule.setContent {
            FitTrackTheme {
                FitTrackNavHost()
            }
        }

        val filterBack = context.getString(R.string.filter_back)
        val filterBodyweight = context.getString(R.string.filter_bodyweight)
        val pullUpName = context.getString(R.string.exercise_name_pull_up)
        val emptyExercisesText = context.getString(R.string.empty_exercises)

        // Select Back (Body Part) and Bodyweight (Equipment) filter chips
        composeTestRule.onNode(hasText(filterBack) and isSelectable()).performClick()
        composeTestRule.onNode(hasText(filterBodyweight) and isSelectable()).performClick()

        // Search "Pull" -> should show Pull Up
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Pull")
        composeTestRule.onAllNodesWithText(pullUpName).onFirst().assertIsDisplayed()

        // Change search query to non-matching -> should show empty state without crash
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("UnmatchedQuery")
        composeTestRule.onNodeWithText(emptyExercisesText).assertIsDisplayed()
    }
}
