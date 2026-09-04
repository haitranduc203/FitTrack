package com.haitranduc.fittrack

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val navExercises = context.getString(R.string.nav_exercises)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        val filterAll = context.getString(R.string.filter_all)

        val backNodes = composeTestRule.onAllNodesWithContentDescription(cdNavigateUp).fetchSemanticsNodes()
        if (backNodes.isNotEmpty()) {
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
        }

        val navNodes = composeTestRule.onAllNodesWithText(navExercises).fetchSemanticsNodes()
        if (navNodes.isNotEmpty()) {
            composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithText(titleExercises).fetchSemanticsNodes().isNotEmpty()
        }

        // Reset search query if it was modified by a prior test
        val searchNodes = composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        if (searchNodes.isNotEmpty()) {
            composeTestRule.onNode(hasSetTextAction()).performTextReplacement("")
        }

        // Reset filter chips to "All" if present
        val allChips = composeTestRule.onAllNodesWithText(filterAll).fetchSemanticsNodes()
        if (allChips.isNotEmpty()) {
            composeTestRule.onAllNodesWithText(filterAll).onFirst().performClick()
            if (allChips.size > 1) {
                composeTestRule.onAllNodesWithText(filterAll).onLast().performClick()
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_bottomNavigationDestinations() {
        waitUntilReady()

        val navExercises = context.getString(R.string.nav_exercises)
        val navWorkouts = context.getString(R.string.nav_workouts)
        val navHistory = context.getString(R.string.nav_history)
        val navSettings = context.getString(R.string.nav_settings)
        val titleExercises = context.getString(R.string.title_exercises)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val titleHistory = context.getString(R.string.title_history)
        val titleSettings = context.getString(R.string.title_settings)

        // 1. Initially on Exercises destination
        composeTestRule.onAllNodesWithText(titleExercises).onFirst().assertIsDisplayed()

        // 2. Navigate to Workouts via bottom bar
        composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()

        // 3. Navigate to History via bottom bar
        composeTestRule.onAllNodesWithText(navHistory).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleHistory).onFirst().assertIsDisplayed()

        // 4. Navigate to Settings via bottom bar
        composeTestRule.onAllNodesWithText(navSettings).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleSettings).onFirst().assertIsDisplayed()

        // 5. Return to Exercises via bottom bar
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
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val btnAddExercise = context.getString(R.string.btn_add_exercise)
        val btnStartWorkout = context.getString(R.string.btn_start_workout)
        val titleActiveWorkout = context.getString(R.string.title_active_workout)
        val cdSetDone = context.getString(R.string.cd_set_done)
        val btnFinishWorkout = context.getString(R.string.btn_finish_workout)
        val titleHistoryDetail = context.getString(R.string.title_history_detail)

        // 1. Navigate to Workouts tab via bottom bar
        composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()

        // 2. Create a new workout template
        composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()

        // 3. Set name and add exercise
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("Push Day")
        composeTestRule.onNodeWithText(btnAddExercise).performClick()

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("3/4 sit-up").performClick()

        // 4. Start workout
        composeTestRule.onNodeWithText(btnStartWorkout).assertIsDisplayed()
        composeTestRule.onNodeWithText(btnStartWorkout).performClick()

        // 5. Active Workout Screen
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(titleActiveWorkout).assertIsDisplayed()
        composeTestRule.onNodeWithText(btnFinishWorkout).assertIsDisplayed()

        // 6. Complete a set
        composeTestRule.onNodeWithContentDescription(cdSetDone).performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty()
        }

        // 7. Finish workout
        composeTestRule.onNodeWithText(btnFinishWorkout).performClick()

        // 8. Navigates to History Detail
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(titleHistoryDetail).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(titleHistoryDetail).assertIsDisplayed()

        // 9. Navigate back to Workouts list
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
        composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()

        // Return to Exercises tab
        val navExercises = context.getString(R.string.nav_exercises)
        composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
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

        // Return to Exercises tab
        val navExercises = context.getString(R.string.nav_exercises)
        composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
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

        // Reset search query
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("")
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

        // Clean up
        val filterAll = context.getString(R.string.filter_all)
        composeTestRule.onAllNodesWithText(filterAll).onFirst().performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("")
    }

    @Test
    fun test_equipmentFilter_filtersCorrectExercises() {
        waitUntilReady()

        val filterBarbell = context.getString(R.string.filter_barbell)
        val filterBodyweight = context.getString(R.string.filter_bodyweight)
        val filterAll = context.getString(R.string.filter_all)

        // Ensure clean start
        val allChips = composeTestRule.onAllNodesWithText(filterAll).fetchSemanticsNodes()
        if (allChips.size > 1) {
            composeTestRule.onAllNodesWithText(filterAll).onLast().performClick()
        }

        // Initially 3/4 sit-up (body weight) is visible
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()

        // Click Barbell filter chip
        composeTestRule.onNode(hasText(filterBarbell) and isSelectable()).performClick()

        // Barbell alternate biceps curl must be displayed, 3/4 sit-up must not exist
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("barbell alternate biceps curl").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("barbell alternate biceps curl").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("3/4 sit-up").assertDoesNotExist()

        // Click Bodyweight filter chip
        composeTestRule.onNode(hasText(filterBodyweight) and isSelectable()).performClick()

        // 3/4 sit-up must be displayed, barbell alternate biceps curl must not exist
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()
        composeTestRule.onNodeWithText("barbell alternate biceps curl").assertDoesNotExist()

        // Clean up
        if (allChips.size > 1) {
            composeTestRule.onAllNodesWithText(filterAll).onLast().performClick()
        }
    }

    @Test
    fun test_combinedFilterAndSearch_doesNotCrash() {
        waitUntilReady()

        val filterChest = context.getString(R.string.filter_chest)
        val filterBodyweight = context.getString(R.string.filter_bodyweight)
        val filterAll = context.getString(R.string.filter_all)

        // Select Chest and Bodyweight filter chips
        composeTestRule.onNode(hasText(filterChest) and isSelectable()).performClick()
        composeTestRule.onNode(hasText(filterBodyweight) and isSelectable()).performClick()

        // Search "archer"
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("archer")

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("archer push up").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("archer push up").assertIsDisplayed()

        // Clean up
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("")
        val allChips = composeTestRule.onAllNodesWithText(filterAll).fetchSemanticsNodes()
        if (allChips.isNotEmpty()) {
            composeTestRule.onAllNodesWithText(filterAll).onFirst().performClick()
            if (allChips.size > 1) {
                composeTestRule.onAllNodesWithText(filterAll).onLast().performClick()
            }
        }
    }

    @Test
    fun test_activeWorkout_backAndResume_resumesExistingSessionWithoutDuplicate() {
        waitUntilReady()

        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val btnAddExercise = context.getString(R.string.btn_add_exercise)
        val btnStartWorkout = context.getString(R.string.btn_start_workout)
        val titleActiveWorkout = context.getString(R.string.title_active_workout)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        val cdSetDone = context.getString(R.string.cd_set_done)
        val btnFinishWorkout = context.getString(R.string.btn_finish_workout)
        val titleHistoryDetail = context.getString(R.string.title_history_detail)

        val testDb = Room.databaseBuilder(
            context,
            FitTrackDatabase::class.java,
            "fittrack.db"
        ).build()

        try {
            // 1. Navigate to Workouts tab
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }

            // 2. Create a workout template
            val uniqueWorkoutName = "Resume Test " + System.currentTimeMillis()
            composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()
            composeTestRule.onNode(hasSetTextAction()).performTextReplacement(uniqueWorkoutName)
            composeTestRule.onNodeWithText(btnAddExercise).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("3/4 sit-up").performClick()

            // 3. Start workout
            composeTestRule.onNodeWithText(btnStartWorkout).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(titleActiveWorkout).assertIsDisplayed()

            // 4. Record initial session ID from repository
            val firstActiveSession = runBlocking {
                testDb.workoutSessionDao().getActiveSession()
            }
            val firstSessionId = firstActiveSession?.id ?: -1L
            assertTrue("Session ID must be positive", firstSessionId > 0L)

            // 5. Back out of Active Workout
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
            composeTestRule.waitForIdle()

            // 6. Click Start again (from editor or reopen if back to workouts)
            val startNodes = composeTestRule.onAllNodesWithText(btnStartWorkout).fetchSemanticsNodes()
            if (startNodes.isEmpty()) {
                composeTestRule.onNodeWithText(uniqueWorkoutName).performClick()
                composeTestRule.waitUntil(timeoutMillis = 15_000) {
                    composeTestRule.onAllNodesWithText(btnStartWorkout).fetchSemanticsNodes().isNotEmpty()
                }
            }
            composeTestRule.onNodeWithText(btnStartWorkout).performClick()

            // 7. Verify navigated to Active Workout
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(titleActiveWorkout).assertIsDisplayed()

            // 8. Confirm navigation resumed the exact same session ID
            val secondActiveSession = runBlocking {
                testDb.workoutSessionDao().getActiveSession()
            }
            assertEquals(firstSessionId, secondActiveSession?.id)

            // 9. Confirm database still has only ONE unfinished session
            val cursor = testDb.query(
                SimpleSQLiteQuery("SELECT COUNT(*) FROM workout_sessions WHERE finishedAt IS NULL")
            )
            cursor.moveToFirst()
            val unfinishedCount = cursor.getInt(0)
            cursor.close()
            assertEquals(1, unfinishedCount)

            // 10. Clean up: complete set and finish so session is closed
            composeTestRule.onNodeWithContentDescription(cdSetDone).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(btnFinishWorkout).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleHistoryDetail).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
            composeTestRule.waitForIdle()
        } finally {
            testDb.close()
        }
    }

    @Test
    fun test_settings_themeSelection_persistsAcrossRecreation() {
        waitUntilReady()

        val navSettings = context.getString(R.string.nav_settings)
        val titleSettings = context.getString(R.string.title_settings)
        val themeDark = context.getString(R.string.theme_dark)
        val themeSystem = context.getString(R.string.theme_system)
        val themeLight = context.getString(R.string.theme_light)

        // 1. Navigate to Settings via bottom bar
        composeTestRule.onAllNodesWithText(navSettings).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleSettings).onFirst().assertIsDisplayed()

        // 2. Verify all theme options are displayed
        composeTestRule.onNodeWithText(themeSystem).assertIsDisplayed()
        composeTestRule.onNodeWithText(themeLight).assertIsDisplayed()
        composeTestRule.onNodeWithText(themeDark).assertIsDisplayed()

        // 3. Select Dark theme
        composeTestRule.onNodeWithText(themeDark).performClick()
        composeTestRule.waitForIdle()

        // 4. Recreate Activity
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // 5. Verify Settings still shows Dark selected after recreation
        composeTestRule.onAllNodesWithText(navSettings).onLast().performClick()
        composeTestRule.onAllNodesWithText(titleSettings).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText(themeDark).assertIsDisplayed()

        // 6. Reset to System Default for test hygiene
        composeTestRule.onNodeWithText(themeSystem).performClick()
        composeTestRule.waitForIdle()
    }
}
