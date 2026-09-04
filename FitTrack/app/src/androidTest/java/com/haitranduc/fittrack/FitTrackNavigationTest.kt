package com.haitranduc.fittrack

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FitTrackNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun waitUntilReady() {
        val titleExercises = context.getString(R.string.title_exercises)
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithText(titleExercises).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun test_bottomNavigationDestinations() {
        waitUntilReady()

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
        waitUntilReady()

        val titleExercises = context.getString(R.string.title_exercises)
        val labelInstructions = context.getString(R.string.label_instructions)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)

        // Verify on Exercises list
        composeTestRule.onAllNodesWithText(titleExercises).onFirst().assertIsDisplayed()

        // Click first item (3/4 sit-up) directly
        composeTestRule.onNodeWithText("3/4 sit-up").performClick()

        // Verify on Detail screen
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(labelInstructions).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(labelInstructions).assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()

        // Verify back on Exercises list
        composeTestRule.onAllNodesWithText(titleExercises).onFirst().assertIsDisplayed()
    }

    @Test
    fun test_workoutList_to_workoutEditor_to_activeWorkout_andFinish() {
        waitUntilReady()

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
        waitUntilReady()

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
        waitUntilReady()

        val emptyExercisesText = context.getString(R.string.empty_exercises)

        // Enter non-matching search query using performTextReplacement
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("XYZNonExistentExercise123")

        // Verify empty state is displayed
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(emptyExercisesText).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(emptyExercisesText).assertIsDisplayed()
    }

    @Test
    fun test_bodyPartFilter_filtersCorrectExercises() {
        waitUntilReady()

        val filterChest = context.getString(R.string.filter_chest)

        // Click Chest filter chip (which is selectable)
        composeTestRule.onNode(hasText(filterChest) and isSelectable()).performClick()

        // Bench press must be displayed when searched
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("bench press")
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("barbell bench press").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("barbell bench press").onFirst().assertIsDisplayed()

        // Deadlift (upper legs) must not exist under chest filter
        composeTestRule.onNode(hasText("barbell deadlift")).assertDoesNotExist()
    }

    @Test
    fun test_equipmentFilter_filtersCorrectExercises() {
        waitUntilReady()

        val filterBarbell = context.getString(R.string.filter_barbell)
        val filterBodyweight = context.getString(R.string.filter_bodyweight)

        // Initially 3/4 sit-up (body weight) is visible
        composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()

        // Click Barbell filter chip
        composeTestRule.onNode(hasText(filterBarbell) and isSelectable()).performClick()

        // Barbell alternate biceps curl must be displayed, 3/4 sit-up must not exist
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("barbell alternate biceps curl").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("barbell alternate biceps curl").assertIsDisplayed()
        composeTestRule.onNodeWithText("3/4 sit-up").assertDoesNotExist()

        // Click Bodyweight filter chip
        composeTestRule.onNode(hasText(filterBodyweight) and isSelectable()).performClick()

        // 3/4 sit-up must be displayed, barbell alternate biceps curl must not exist
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()
        composeTestRule.onNodeWithText("barbell alternate biceps curl").assertDoesNotExist()
    }

    @Test
    fun test_combinedFilterAndSearch_doesNotCrash() {
        waitUntilReady()

        val filterChest = context.getString(R.string.filter_chest)
        val filterBodyweight = context.getString(R.string.filter_bodyweight)

        // Select Chest and Bodyweight filter chips
        composeTestRule.onNode(hasText(filterChest) and isSelectable()).performClick()
        composeTestRule.onNode(hasText(filterBodyweight) and isSelectable()).performClick()

        // Search "archer"
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("archer")

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("archer push up").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("archer push up").assertIsDisplayed()
    }
}
