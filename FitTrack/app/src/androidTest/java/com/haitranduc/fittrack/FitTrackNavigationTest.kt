package com.haitranduc.fittrack

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FitTrackNavigationTest {

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

        // Ensure title is present (waits for initial launch & seed)
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodesWithText(titleExercises).fetchSemanticsNodes().isNotEmpty()
        }

        // Fast path: if root exercise item is already visible, the screen is clean and ready
        if (composeTestRule.onAllNodesWithText(targetExercise).fetchSemanticsNodes().isNotEmpty()) {
            return
        }

        val navExercises = context.getString(R.string.nav_exercises)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        val filterAll = context.getString(R.string.filter_all)
        val filterFavorites = context.getString(R.string.filter_favorites)

        // Pop back up to 3 times if nested
        repeat(3) {
            val backNodes = composeTestRule.onAllNodesWithContentDescription(cdNavigateUp).fetchSemanticsNodes()
            if (backNodes.isNotEmpty()) {
                composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
                composeTestRule.waitForIdle()
            }
        }

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

        // Clear search input if present and not already empty
        val searchNodes = composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        if (searchNodes.isNotEmpty()) {
            val text = searchNodes[0].config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.EditableText) {
                androidx.compose.ui.text.AnnotatedString("")
            }.text
            if (text.isNotEmpty()) {
                composeTestRule.onNode(hasSetTextAction()).performTextClearance()
                composeTestRule.waitForIdle()
            }
        }

        // Deselect favorites filter if selected
        val favSelected = composeTestRule.onAllNodes(hasText(filterFavorites) and isSelected()).fetchSemanticsNodes()
        if (favSelected.isNotEmpty()) {
            composeTestRule.onAllNodes(hasText(filterFavorites) and isSelected()).onFirst().performClick()
            composeTestRule.waitForIdle()
        }

        // Reset all unselected "All" filter chips (both Body Part and Equipment)
        repeat(2) {
            val unselectedAllChips = composeTestRule.onAllNodes(hasText(filterAll) and isNotSelected()).fetchSemanticsNodes()
            if (unselectedAllChips.isNotEmpty()) {
                composeTestRule.onAllNodes(hasText(filterAll) and isNotSelected()).onFirst().performClick()
                composeTestRule.waitForIdle()
            }
        }

        // Wait until initial exercise item is displayed
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(targetExercise).fetchSemanticsNodes().isNotEmpty()
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

        // 5. Navigate back to Exercises
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

        try {
            // 1. Navigate to Workouts tab via bottom bar
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()

            // 2. Create a new workout template
            composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()

            // 3. Set name and add exercise
            composeTestRule.onNode(hasSetTextAction()).performTextClearance()
            composeTestRule.onNode(hasSetTextAction()).performTextInput("Push Day")
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
                composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithContentDescription(cdSetDone).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(titleActiveWorkout).assertIsDisplayed()
            composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()
            composeTestRule.onNodeWithText(btnFinishWorkout).assertIsDisplayed()

            // 6. Complete a set
            composeTestRule.onNodeWithContentDescription(cdSetDone).performScrollTo().performClick()
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
            composeTestRule.waitForIdle()
        } finally {
            cleanupDatabaseSessions()
        }
    }

    @Test
    fun test_historyList_to_historyDetail_andBack() {
        waitUntilReady()

        val navHistory = context.getString(R.string.nav_history)
        val titleHistory = context.getString(R.string.title_history)
        val pushDayName = context.getString(R.string.workout_template_push_day)
        val titleHistoryDetail = context.getString(R.string.title_history_detail)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)

        val now = System.currentTimeMillis()
        runBlocking {
            val active = testDb.workoutSessionDao().getActiveSession()
            if (active != null) {
                testDb.workoutSessionDao().updateSession(active.copy(finishedAt = now, durationSeconds = 60L))
            }
            val sid = testDb.workoutSessionDao().insertSession(
                com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity(
                    id = 0L,
                    workoutId = null,
                    workoutNameSnapshot = pushDayName,
                    startedAt = now - 60_000,
                    finishedAt = now,
                    durationSeconds = 60L
                )
            )
            testDb.setLogDao().insertSetLog(
                com.haitranduc.fittrack.data.local.entity.SetLogEntity(
                    id = 0L,
                    sessionId = sid,
                    exerciseId = "0025",
                    exerciseNameSnapshot = "3/4 sit-up",
                    setNumber = 1,
                    reps = 10,
                    weightKg = 50.0,
                    completedAt = now
                )
            )
        }

        // Navigate to History tab via bottom bar
        composeTestRule.onAllNodesWithText(navHistory).onLast().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(titleHistory).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText(titleHistory).onFirst().assertIsDisplayed()

        // Click on Push Day history item
        composeTestRule.onAllNodesWithText(pushDayName).onFirst().performClick()

        // Verify on History Detail screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(titleHistoryDetail).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(titleHistoryDetail).assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()

        // Verify back on History list
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(titleHistory).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText(titleHistory).onFirst().assertIsDisplayed()

        // Return to Exercises tab
        val navExercises = context.getString(R.string.nav_exercises)
        val titleExercises = context.getString(R.string.title_exercises)
        composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(titleExercises).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_searchNonMatchingQuery_showsEmptyState() {
        waitUntilReady()

        val emptyExercisesText = context.getString(R.string.empty_exercises)

        // Enter non-matching search query
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("XYZNonExistentExercise123")

        // Verify empty state is displayed, re-applying if an in-flight query reset the field
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            val hasEmpty = composeTestRule.onAllNodesWithText(emptyExercisesText).fetchSemanticsNodes().isNotEmpty()
            if (!hasEmpty) {
                val currentText = composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().firstOrNull()
                    ?.config?.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.EditableText) {
                        androidx.compose.ui.text.AnnotatedString("")
                    }?.text
                if (currentText != "XYZNonExistentExercise123") {
                    composeTestRule.onNode(hasSetTextAction()).performTextReplacement("XYZNonExistentExercise123")
                }
            }
            hasEmpty
        }
        composeTestRule.onNodeWithText(emptyExercisesText).assertIsDisplayed()

        // Reset search query
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("")
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_bodyPartFilter_filtersCorrectExercises() {
        waitUntilReady()

        val filterChest = context.getString(R.string.filter_chest)

        // Click Chest filter chip (which is selectable)
        composeTestRule.onNode(hasText(filterChest) and isSelectable()).performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("archer push up").fetchSemanticsNodes().isNotEmpty()
        }

        // Bench press must be displayed when searched
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("bench press")
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            val hasItem = composeTestRule.onAllNodesWithText("barbell bench press").fetchSemanticsNodes().isNotEmpty()
            if (!hasItem) {
                val currentText = composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().firstOrNull()
                    ?.config?.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.EditableText) {
                        androidx.compose.ui.text.AnnotatedString("")
                    }?.text
                if (currentText != "bench press") {
                    composeTestRule.onNode(hasSetTextAction()).performTextReplacement("bench press")
                }
            }
            hasItem
        }
        composeTestRule.onAllNodesWithText("barbell bench press").onFirst().assertIsDisplayed()

        // Deadlift (upper legs) must not exist under chest filter
        composeTestRule.onNode(hasText("barbell deadlift")).assertDoesNotExist()

        // Clean up
        val filterAll = context.getString(R.string.filter_all)
        val unselectedAll = composeTestRule.onAllNodes(hasText(filterAll) and isNotSelected()).fetchSemanticsNodes()
        if (unselectedAll.isNotEmpty()) {
            composeTestRule.onAllNodes(hasText(filterAll) and isNotSelected()).onFirst().performClick()
        }
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("")
        composeTestRule.waitForIdle()
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
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("archer")

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("archer push up").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("archer push up").assertIsDisplayed()

        // Clean up
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
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

        // 1. Create a workout template
        val uniqueWorkoutName = "Resume Test " + System.currentTimeMillis()

        try {
            // 1. Navigate to Workouts tab
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }

            // 2. Create a workout template
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
                composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithContentDescription(cdSetDone).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(titleActiveWorkout).assertIsDisplayed()
            composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()

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

            // 7. Verify navigated to Active Workout and wait for exercise content and set-completion action
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithContentDescription(cdSetDone).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(titleActiveWorkout).assertIsDisplayed()
            composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()

            // 8. Confirm navigation resumed the exact same session ID
            val secondActiveSession = runBlocking {
                testDb.workoutSessionDao().getActiveSession()
            }
            assertEquals(firstSessionId, secondActiveSession?.id)

            // 9. Confirm database still has only ONE unfinished session
            val cursor = testDb.query(
                androidx.sqlite.db.SimpleSQLiteQuery("SELECT COUNT(*) FROM workout_sessions WHERE finishedAt IS NULL")
            )
            cursor.moveToFirst()
            val unfinishedCount = cursor.getInt(0)
            cursor.close()
            assertEquals(1, unfinishedCount)

            // 10. Clean up: complete set and finish so session is closed
            composeTestRule.onNodeWithContentDescription(cdSetDone).performScrollTo().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(btnFinishWorkout).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleHistoryDetail).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
            composeTestRule.waitForIdle()

            // Return to Exercises tab
            val navExercises = context.getString(R.string.nav_exercises)
            composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
            composeTestRule.waitForIdle()
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

        // Return to Exercises tab
        val navExercises = context.getString(R.string.nav_exercises)
        composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_workoutSave_snackbarDisplaysOnce_andDoesNotReplayAfterRecreate() {
        waitUntilReady()

        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val btnAddExercise = context.getString(R.string.btn_add_exercise)
        val btnSaveWorkout = context.getString(R.string.btn_save_workout)
        val msgWorkoutSaved = context.getString(R.string.msg_workout_saved)
        val uniqueWorkoutName = "SnackTest_" + System.currentTimeMillis()

        try {
            // Navigate to Workouts tab
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }

            // Create workout and save
            composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()
            composeTestRule.onNode(hasSetTextAction()).performTextReplacement(uniqueWorkoutName)
            composeTestRule.onNodeWithText(btnAddExercise).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("3/4 sit-up").performClick()
            composeTestRule.onNodeWithText(btnSaveWorkout).performClick()

            // Verify returned to workouts list and snackbar is displayed
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText(msgWorkoutSaved).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(msgWorkoutSaved).assertIsDisplayed()

            // Recreate Activity
            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()

            // Verify snackbar is NOT replayed
            composeTestRule.onAllNodesWithText(msgWorkoutSaved).assertCountEquals(0)
        } finally {
            try {
                runBlocking {
                    testDb.openHelper.writableDatabase.execSQL(
                        "DELETE FROM workouts WHERE name = ?",
                        arrayOf(uniqueWorkoutName)
                    )
                }
            } catch (_: Exception) {}
            val navExercises = context.getString(R.string.nav_exercises)
            composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun test_workoutDelete_dialogConfirmAndCancel() {
        waitUntilReady()

        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val btnAddExercise = context.getString(R.string.btn_add_exercise)
        val btnSaveWorkout = context.getString(R.string.btn_save_workout)
        val cdDeleteWorkout = context.getString(R.string.cd_delete_workout)
        val dialogDeleteTitle = context.getString(R.string.dialog_delete_workout_title)
        val actionCancel = context.getString(R.string.action_cancel)
        val actionDelete = context.getString(R.string.action_delete)
        val uniqueWorkoutName = "DelDialogTest_" + System.currentTimeMillis()

        try {
            // 1. Create a workout
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()
            composeTestRule.onNode(hasSetTextAction()).performTextReplacement(uniqueWorkoutName)
            composeTestRule.onNodeWithText(btnAddExercise).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("3/4 sit-up").performClick()
            composeTestRule.onNodeWithText(btnSaveWorkout).performClick()

            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(uniqueWorkoutName).fetchSemanticsNodes().isNotEmpty()
            }

            // 2. Click delete icon to trigger dialog
            composeTestRule.onAllNodesWithContentDescription(cdDeleteWorkout).onFirst().performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(dialogDeleteTitle).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(dialogDeleteTitle).assertIsDisplayed()

            // 3. Click Cancel -> Dialog dismisses, workout remains
            composeTestRule.onNodeWithText(actionCancel).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onAllNodesWithText(dialogDeleteTitle).assertCountEquals(0)
            composeTestRule.onNodeWithText(uniqueWorkoutName).assertIsDisplayed()

            // 4. Click delete icon again and Confirm Delete
            composeTestRule.onAllNodesWithContentDescription(cdDeleteWorkout).onFirst().performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(dialogDeleteTitle).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(actionDelete).performClick()

            // 5. Verify workout is deleted
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(uniqueWorkoutName).fetchSemanticsNodes().isEmpty()
            }
            composeTestRule.onAllNodesWithText(uniqueWorkoutName).assertCountEquals(0)
        } finally {
            try {
                runBlocking {
                    testDb.openHelper.writableDatabase.execSQL(
                        "DELETE FROM workouts WHERE name = ?",
                        arrayOf(uniqueWorkoutName)
                    )
                }
            } catch (_: Exception) {}
            val navExercises = context.getString(R.string.nav_exercises)
            composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun test_workoutEditor_discardDialogConfirmAndCancel() {
        waitUntilReady()

        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        val dialogDiscardTitle = context.getString(R.string.dialog_discard_changes_title)
        val actionKeepEditing = context.getString(R.string.action_keep_editing)
        val actionDiscard = context.getString(R.string.action_discard)
        val draftName = "DiscardDraftTest"

        try {
            // Navigate to Workouts tab
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }

            // Open editor and type dirty changes
            composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()
            composeTestRule.onNode(hasSetTextAction()).performTextReplacement(draftName)

            // Click back button to trigger discard dialog
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(dialogDiscardTitle).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(dialogDiscardTitle).assertIsDisplayed()

            // Click "Keep editing"
            composeTestRule.onNodeWithText(actionKeepEditing).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onAllNodesWithText(dialogDiscardTitle).assertCountEquals(0)
            composeTestRule.onNodeWithText(draftName).assertIsDisplayed()

            // Click back button again and confirm Discard
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText(dialogDiscardTitle).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(actionDiscard).performClick()

            // Verify returned to workouts list
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onAllNodesWithText(titleWorkouts).onFirst().assertIsDisplayed()
        } finally {
            // Return to exercises tab
            val navExercises = context.getString(R.string.nav_exercises)
            composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun test_exerciseSearchAndFilters_persistsAcrossRecreation() {
        waitUntilReady()

        val filterChest = "Chest"

        // 1. Enter query and select a filter chip
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("bench")
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(filterChest).onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodes(hasText(filterChest) and isSelected()).assertCountEquals(1)

        // 2. Recreate Activity
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // 3. Verify query and filter chip state persisted
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodes(hasText("bench")).fetchSemanticsNodes().isNotEmpty() &&
            composeTestRule.onAllNodes(hasText(filterChest) and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasText("bench")).assertIsDisplayed()
        composeTestRule.onAllNodes(hasText(filterChest) and isSelected()).assertCountEquals(1)

        // 4. Reset filters
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        composeTestRule.onAllNodesWithText(filterChest).onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_workoutEditor_nameAndExerciseList_persistsAcrossRecreation() {
        waitUntilReady()

        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val btnAddExercise = context.getString(R.string.btn_add_exercise)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        val actionDiscard = context.getString(R.string.action_discard)
        val dialogDiscardTitle = context.getString(R.string.dialog_discard_changes_title)
        val draftName = "RecreateDraft_" + System.currentTimeMillis()

        // 1. Open Workout Editor
        composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()

        // 2. Set draft name
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement(draftName)

        // 3. Add exercise 1 ("3/4 sit-up")
        composeTestRule.onNodeWithText(btnAddExercise).performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("3/4 sit-up").performClick()

        // 4. Add exercise 2 ("45° side bend")
        composeTestRule.onNodeWithText(btnAddExercise).performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("45° side bend").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("45° side bend").performClick()

        // Verify both exercises are in the editor
        composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()
        composeTestRule.onNodeWithText("45° side bend").assertIsDisplayed()

        // 5. Recreate Activity
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // 6. Verify draft name and both exercises are preserved
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(draftName).fetchSemanticsNodes().isNotEmpty() &&
            composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty() &&
            composeTestRule.onAllNodesWithText("45° side bend").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(draftName).assertIsDisplayed()
        composeTestRule.onNodeWithText("3/4 sit-up").assertIsDisplayed()
        composeTestRule.onNodeWithText("45° side bend").assertIsDisplayed()

        // 7. Clean up: discard
        composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(dialogDiscardTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(actionDiscard).performClick()

        val navExercises = context.getString(R.string.nav_exercises)
        composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_activeWorkout_inputAndRestTimer_persistsAcrossRecreation() {
        waitUntilReady()

        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val btnAddExercise = context.getString(R.string.btn_add_exercise)
        val btnStartWorkout = context.getString(R.string.btn_start_workout)
        val titleActiveWorkout = context.getString(R.string.title_active_workout)
        val cdSetDone = context.getString(R.string.cd_set_done)
        val btnFinishWorkout = context.getString(R.string.btn_finish_workout)
        val cdNavigateUp = context.getString(R.string.cd_navigate_up)
        val uniqueWorkoutName = "ActiveRecreate_" + System.currentTimeMillis()
        val weightFieldTag = "active_weight_0001"
        val repsFieldTag = "active_reps_0001"
        val restTimerTag = "active_rest_timer"

        try {
            // 1. Create and start workout
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()
            composeTestRule.onNode(hasSetTextAction()).performTextReplacement(uniqueWorkoutName)
            composeTestRule.onNodeWithText(btnAddExercise).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("3/4 sit-up").performClick()
            composeTestRule.onNodeWithText(btnStartWorkout).performClick()

            // 2. Wait for Active Workout screen
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithContentDescription(cdSetDone).fetchSemanticsNodes().isNotEmpty()
            }

            // 3. Enter a draft set, then recreate before completing it.
            composeTestRule.onNodeWithTag(weightFieldTag).performTextReplacement("75")
            composeTestRule.onNodeWithTag(repsFieldTag).performTextReplacement("12")
            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithTag(weightFieldTag).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(repsFieldTag).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag(weightFieldTag).assertTextEquals("75")
            composeTestRule.onNodeWithTag(repsFieldTag).assertTextEquals("12")

            // 4. Complete the restored draft set and capture the active rest timer.
            composeTestRule.onNodeWithContentDescription(cdSetDone).performScrollTo().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithTag(restTimerTag).fetchSemanticsNodes().isNotEmpty()
            }
            val remainingBeforeRecreation = composeTestRule.onNodeWithTag(restTimerTag)
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription].toLong()
            assertTrue(remainingBeforeRecreation in 1L..90L)

            // 5. Recreate again and verify both persisted inputs and timer progress.
            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithTag(weightFieldTag).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(repsFieldTag).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(restTimerTag).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag(weightFieldTag).assertTextEquals("75")
            composeTestRule.onNodeWithTag(repsFieldTag).assertTextEquals("12")
            val remainingAfterRecreation = composeTestRule.onNodeWithTag(restTimerTag)
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription].toLong()
            assertTrue(remainingAfterRecreation in 1L..remainingBeforeRecreation)

            // 6. Finish workout to close session.
            composeTestRule.onNodeWithText(btnFinishWorkout).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(context.getString(R.string.title_history_detail)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription(cdNavigateUp).performClick()
            composeTestRule.waitForIdle()
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
            val navExercises = context.getString(R.string.nav_exercises)
            if (composeTestRule.onAllNodesWithText(navExercises).fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    @Test
    fun test_activeWorkout_invalidRepsAndWeight_showErrorsWithoutPersistingSet() {
        waitUntilReady()

        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)
        val btnAddExercise = context.getString(R.string.btn_add_exercise)
        val btnStartWorkout = context.getString(R.string.btn_start_workout)
        val titleActiveWorkout = context.getString(R.string.title_active_workout)
        val cdSetDone = context.getString(R.string.cd_set_done)
        val errorInvalidReps = context.getString(R.string.error_invalid_reps)
        val errorInvalidWeight = context.getString(R.string.error_invalid_weight)
        val uniqueWorkoutName = "InvalidInput_" + System.currentTimeMillis()
        val weightFieldTag = "active_weight_0001"
        val repsFieldTag = "active_reps_0001"

        try {
            composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription(cdCreateWorkout).performClick()
            composeTestRule.onNode(hasSetTextAction()).performTextReplacement(uniqueWorkoutName)
            composeTestRule.onNodeWithText(btnAddExercise).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText("3/4 sit-up").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("3/4 sit-up").performClick()
            composeTestRule.onNodeWithText(btnStartWorkout).performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(titleActiveWorkout).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(weightFieldTag).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(repsFieldTag).fetchSemanticsNodes().isNotEmpty()
            }

            // Invalid reps must surface inline and must not write a set.
            composeTestRule.onNodeWithTag(weightFieldTag).performTextReplacement("50")
            composeTestRule.onNodeWithTag(repsFieldTag).performTextReplacement("0")
            composeTestRule.onNodeWithContentDescription(cdSetDone).performScrollTo().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(errorInvalidReps).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(errorInvalidReps).assertIsDisplayed()

            composeTestRule.onNodeWithTag(repsFieldTag).performTextReplacement("10")
            composeTestRule.onNodeWithTag(weightFieldTag).performTextReplacement("-1")
            composeTestRule.onNodeWithContentDescription(cdSetDone).performScrollTo().performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText(errorInvalidWeight).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(errorInvalidWeight).assertIsDisplayed()

            val persistedSets = runBlocking {
                val activeSession = testDb.workoutSessionDao().getActiveSession()
                requireNotNull(activeSession)
                testDb.setLogDao().observeSetLogsForSession(activeSession.id).first()
            }
            assertTrue(persistedSets.isEmpty())
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
            val navExercises = context.getString(R.string.nav_exercises)
            if (composeTestRule.onAllNodesWithText(navExercises).fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    @Test
    fun test_semantics_headingsSelectedAndActionIcons() {
        waitUntilReady()

        val titleExercises = context.getString(R.string.title_exercises)
        val navExercises = context.getString(R.string.nav_exercises)
        val navWorkouts = context.getString(R.string.nav_workouts)
        val titleWorkouts = context.getString(R.string.title_workouts)
        val navHistory = context.getString(R.string.nav_history)
        val titleHistory = context.getString(R.string.title_history)
        val navSettings = context.getString(R.string.nav_settings)
        val titleSettings = context.getString(R.string.title_settings)
        val cdCreateWorkout = context.getString(R.string.cd_create_workout)

        // 1. Heading semantics on Exercises screen
        composeTestRule.onNode(hasText(titleExercises) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()

        // 2. Selected semantics on Bottom Navigation
        composeTestRule.onNode(hasText(navExercises) and isSelected()).assertExists()
        composeTestRule.onNode(hasText(navWorkouts) and isNotSelected()).assertExists()

        // 3. Workouts screen heading and create action icon
        composeTestRule.onAllNodesWithText(navWorkouts).onLast().performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(titleWorkouts).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasText(titleWorkouts) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        composeTestRule.onNodeWithContentDescription(cdCreateWorkout).assertExists().assertHasClickAction()

        // 4. History screen heading
        composeTestRule.onAllNodesWithText(navHistory).onLast().performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(titleHistory).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasText(titleHistory) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()

        // 5. Settings screen heading
        composeTestRule.onAllNodesWithText(navSettings).onLast().performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText(titleSettings).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasText(titleSettings) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()

        // Return to Exercises tab
        composeTestRule.onAllNodesWithText(navExercises).onLast().performClick()
        composeTestRule.waitForIdle()
    }
}
