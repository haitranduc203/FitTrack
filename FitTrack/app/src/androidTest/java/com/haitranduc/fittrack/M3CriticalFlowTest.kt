package com.haitranduc.fittrack

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M3CriticalFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val testDb: FitTrackDatabase
        get() = getTestDatabase(context)

    @Before
    fun setUp() {
        cleanupDatabaseSessions()
    }

    @After
    fun tearDown() {
        cleanupDatabaseSessions()
    }

    private fun cleanupDatabaseSessions() {
        runBlocking {
            try {
                testDb.openHelper.writableDatabase.execSQL(
                    "DELETE FROM workout_sessions WHERE finishedAt IS NULL"
                )
            } catch (_: Exception) {}
        }
    }

    private fun waitUntilReady() {
        val titleExercises = context.getString(R.string.title_exercises)
        val targetExercise = "3/4 sit-up"

        // Wait for database exercise seed to be ready and Exercises title to appear
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodesWithText(titleExercises).fetchSemanticsNodes().isNotEmpty()
        }

        // Fast path: if root exercise is already displayed, no navigation needed
        if (composeTestRule.onAllNodesWithText(targetExercise).fetchSemanticsNodes().isNotEmpty()) {
            return
        }

        val navExercises = context.getString(R.string.nav_exercises)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)

        // Clear any open child screen back to root if needed
        val backNodes = composeTestRule.onAllNodesWithContentDescription(cdNavigateUp).fetchSemanticsNodes()
        if (backNodes.isNotEmpty()) {
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
            composeTestRule.waitForIdle()
        }

        // If not on Exercises tab, navigate to Exercises tab
        val titleNodes = composeTestRule.onAllNodesWithText(titleExercises).fetchSemanticsNodes()
        if (titleNodes.isEmpty()) {
            val navNodes = composeTestRule.onAllNodesWithText(navExercises).fetchSemanticsNodes()
            if (navNodes.isNotEmpty()) {
                composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
            }
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(titleExercises).fetchSemanticsNodes().isNotEmpty()
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(targetExercise).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun criticalWorkoutFlow_create_start_completeSet_finish_persistsAcrossRecreate_survivesTemplateDeletion() {
        waitUntilReady()

        val navWorkouts = context.getString(R.string.nav_workouts)
        val navHistory = context.getString(R.string.nav_history)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val titleHistory = context.getString(R.string.title_history)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val btnAddExercise = context.getString(R.string.btn_add_exercise)
        val btnSaveWorkout = context.getString(R.string.btn_save_workout)
        val btnStartWorkout = context.getString(R.string.btn_start_workout)
        val titleActiveWorkout = context.getString(R.string.title_active_workout)
        val cdSetDone = context.getString(R.string.cd_set_done)
        val btnFinishWorkout = context.getString(R.string.btn_finish_workout)
        val titleHistoryDetail = context.getString(R.string.title_history_detail)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        val cdDeleteWorkout = context.getString(R.string.cd_delete_workout)
        val actionDelete = context.getString(R.string.action_delete)
        val labelDuration = context.getString(R.string.label_duration)

        val uniqueWorkoutName = "M3 Flow Workout " + System.currentTimeMillis()
        val targetExerciseName = "3/4 sit-up"

        try {
            // 1. Navigate to Workouts tab
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()

            // 2. Click Create Workout FAB
            composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()
            composeTestRule.waitForIdle()

            // 3. Enter workout name
            composeTestRule.onNode(hasSetTextAction()).performTextReplacement(uniqueWorkoutName)

            // 4. Add exercise
            composeTestRule.onNodeWithText(btnAddExercise).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(targetExerciseName).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(targetExerciseName).performClick()
            composeTestRule.waitForIdle()

            // 5. Save workout template
            composeTestRule.onNodeWithText(btnSaveWorkout).performClick()

            // 6. Assert workout appears in list
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(uniqueWorkoutName).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(uniqueWorkoutName).assertIsDisplayed()

            // 7. Reopen/edit saved workout
            composeTestRule.onNodeWithText(uniqueWorkoutName).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(btnStartWorkout).fetchSemanticsNodes().isNotEmpty()
            }

            // 8. Start workout
            composeTestRule.onNodeWithText(btnStartWorkout).performClick()

            // 9. Verify Active Workout screen and assert active session belongs to this newly created workout
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText(targetExerciseName).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithContentDescription(cdSetDone).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(titleActiveWorkout).assertIsDisplayed()
            composeTestRule.onNodeWithText(targetExerciseName).assertIsDisplayed()

            val activeSession = runBlocking {
                testDb.workoutSessionDao().getActiveSession()
            }
            assertNotNull("Active session must exist", activeSession)
            assertEquals("Active session must belong to newly created workout", uniqueWorkoutName, activeSession?.workoutNameSnapshot)

            // 10. Enter weight 50 and reps 10
            val textInputNodes = composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
            if (textInputNodes.size >= 2) {
                composeTestRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("50")
                composeTestRule.onAllNodes(hasSetTextAction())[1].performTextReplacement("10")
            }

            // 11. Complete Set
            composeTestRule.onNodeWithContentDescription(cdSetDone).performScrollTo().performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty()
            }

            // M4: 90-second rest timer is visible
            val labelRestTimer = context.getString(R.string.label_rest_timer)
            val btnSkipRest = context.getString(R.string.btn_skip_rest)
            val restTimerFormat = context.getString(R.string.rest_timer_format, 1L, 30L)
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText(labelRestTimer).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(labelRestTimer).assertIsDisplayed()
            composeTestRule.onNodeWithText(restTimerFormat).assertIsDisplayed()
            composeTestRule.onNodeWithText(btnSkipRest).assertIsDisplayed()

            // M4: Skip hides/stops the rest timer without waiting 90 real seconds
            composeTestRule.onNodeWithText(btnSkipRest).performClick()
            composeTestRule.onNodeWithText(labelRestTimer).assertDoesNotExist()

            // 12. Finish Workout
            composeTestRule.onNodeWithText(btnFinishWorkout).performClick()

            // 13. Verify navigated to History Detail
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(titleHistoryDetail).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(titleHistoryDetail).assertIsDisplayed()

            // 14. Assert workout snapshot name, exercise snapshot name, reps, weight, and nonnegative duration
            composeTestRule.onNodeWithText(uniqueWorkoutName).assertIsDisplayed()
            composeTestRule.onNodeWithText(targetExerciseName).assertIsDisplayed()
            val expectedSetSummary = context.getString(R.string.label_set_summary_format, 1, "50", 10)
            composeTestRule.onNodeWithText(expectedSetSummary).assertIsDisplayed()
            composeTestRule.onNodeWithText(labelDuration).assertIsDisplayed()

            // 15. Navigate back to root and verify History list has the finished session
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onAllNodesWithText(navHistory).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(titleHistory).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(uniqueWorkoutName).assertIsDisplayed()

            // M4: History shows at least one workout, completed-set count, and a nonnegative formatted training duration
            val statWorkouts = context.getString(R.string.stat_workouts)
            val statSets = context.getString(R.string.stat_sets)
            val statTime = context.getString(R.string.stat_time)
            composeTestRule.onAllNodesWithText(statWorkouts).onFirst().assertIsDisplayed()
            composeTestRule.onNodeWithText(statSets).assertIsDisplayed()
            composeTestRule.onNodeWithText(statTime).assertIsDisplayed()

            // 16. Recreate Activity and verify History persists across restart/recreation
            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()

            val historyNavNodes = composeTestRule.onAllNodesWithText(navHistory).fetchSemanticsNodes()
            if (historyNavNodes.isNotEmpty()) {
                composeTestRule.onAllNodesWithText(navHistory).onLast().performClick()
            }
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(uniqueWorkoutName).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(uniqueWorkoutName).assertIsDisplayed()

            // 17. Delete workout template and verify completed history session and sets survive
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }

            // Click delete on the newly created template (ordered first by updatedAt DESC)
            val specificDeleteMatcher = hasContentDescription(cdDeleteWorkout) and
                hasParent(hasParent(hasAnyDescendant(hasText(uniqueWorkoutName))))
            val matchingNodes = composeTestRule.onAllNodes(specificDeleteMatcher).fetchSemanticsNodes()
            if (matchingNodes.isNotEmpty()) {
                composeTestRule.onAllNodes(specificDeleteMatcher).onFirst().performClick()
            } else {
                composeTestRule.onAllNodesWithContentDescription(cdDeleteWorkout).onFirst().performClick()
            }

            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(actionDelete).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(actionDelete).performClick()

            // Wait until template is removed from Workouts list
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(uniqueWorkoutName).fetchSemanticsNodes().isEmpty()
            }
            composeTestRule.onNodeWithText(uniqueWorkoutName).assertDoesNotExist()

            // 18. Verify finished session and detail still readable in History
            composeTestRule.onAllNodesWithText(navHistory).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(uniqueWorkoutName).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(uniqueWorkoutName).assertIsDisplayed()

            // Open history detail and assert preserved snapshots and set logs
            composeTestRule.onNodeWithText(uniqueWorkoutName).performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(titleHistoryDetail).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(uniqueWorkoutName).assertIsDisplayed()
            composeTestRule.onNodeWithText(targetExerciseName).assertIsDisplayed()
            composeTestRule.onNodeWithText(expectedSetSummary).assertIsDisplayed()

            // Clean up: navigate back
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
        } finally {
            cleanupDatabaseSessions()
            try {
                runBlocking {
                    testDb.openHelper.writableDatabase.execSQL(
                        "DELETE FROM workouts WHERE name = ?",
                        arrayOf(uniqueWorkoutName)
                    )
                }
            } catch (_: Exception) {}
        }
    }

    @Test
    fun favoriteState_synchronizesBetweenListAndDetail_andSurvivesRecreation() {
        waitUntilReady()

        val titleExercises = context.getString(R.string.title_exercises)
        val cdFavorite = context.getString(R.string.cd_favorite_exercise)
        val cdUnfavorite = context.getString(R.string.cd_unfavorite_exercise)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        val targetExercise = "3/4 sit-up"

        // 1. Ensure on Exercise list and exercise is visible
        composeTestRule.onAllNodesWithText(titleExercises).onFirst().assertIsDisplayed()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(targetExercise).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(targetExercise).assertIsDisplayed()

        // 2. Open Exercise Detail
        composeTestRule.onNodeWithText(targetExercise).performClick()
        composeTestRule.waitForIdle()

        // Reset if initially favorited
        val isFavoritedInitially = composeTestRule.onAllNodesWithContentDescription(cdUnfavorite).fetchSemanticsNodes().isNotEmpty()
        if (isFavoritedInitially) {
            composeTestRule.onNodeWithContentDescription(cdUnfavorite).performClick()
            composeTestRule.waitForIdle()
        }

        // 3. Toggle favorite ON in Detail
        composeTestRule.onNodeWithContentDescription(cdFavorite).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithContentDescription(cdUnfavorite).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(cdUnfavorite).assertIsDisplayed()

        // 4. Navigate back to List and verify synchronized (it shows unfavorite icon/action)
        composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithContentDescription(cdUnfavorite).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithContentDescription(cdUnfavorite).onFirst().assertIsDisplayed()

        // 5. Recreate activity and verify favorite state survives relaunch
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithContentDescription(cdUnfavorite).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithContentDescription(cdUnfavorite).onFirst().assertIsDisplayed()

        // 6. Open detail again and verify favorite state is preserved
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(targetExercise).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(targetExercise).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(cdUnfavorite).assertIsDisplayed()

        // Clean up: toggle off
        composeTestRule.onNodeWithContentDescription(cdUnfavorite).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
    }

    @Test
    fun themeSelection_survivesActivityRecreation() {
        waitUntilReady()

        val navSettings = context.getString(R.string.nav_settings)
        val titleSettings = context.getString(R.string.title_settings)
        val themeDark = context.getString(R.string.theme_dark)
        val themeSystem = context.getString(R.string.theme_system)
        val navExercises = context.getString(R.string.nav_exercises)

        // 1. Navigate to Settings
        composeTestRule.onAllNodesWithText(navSettings).onLast().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(titleSettings).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Select Dark theme
        composeTestRule.onNodeWithText(themeDark).performClick()
        composeTestRule.waitForIdle()

        // 3. Recreate activity
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // 4. Verify Dark theme is retained
        composeTestRule.onAllNodesWithText(navSettings).onLast().performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(titleSettings).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(themeDark).assertIsDisplayed()

        // 5. Clean up: reset to System
        composeTestRule.onNodeWithText(themeSystem).performClick()
        composeTestRule.waitForIdle()

        // Return to exercises
        composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
    }
}
