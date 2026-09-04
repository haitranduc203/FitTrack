package com.haitranduc.fittrack.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FitTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_preservesExistingDataAndCreatesFavoriteExercisesTable() {
        // 1. Create v1 database and insert sample data into exercises, workouts, sessions, sets
        var db = helper.createDatabase(testDb, 1).apply {
            execSQL(
                """
                INSERT INTO exercises (id, name, bodyPart, equipment, target, muscleGroup, secondaryMuscles, instructions, imagePath, gifPath)
                VALUES ('e1', 'Bench Press', 'chest', 'barbell', 'pectorals', 'chest', '["triceps"]', '["Push"]', NULL, NULL)
                """.trimIndent()
            )
            execSQL("INSERT INTO workouts (id, name, createdAt, updatedAt) VALUES (1, 'Push Day', 1000, 1000)")
            execSQL("INSERT INTO workout_exercises (workoutId, exerciseId, orderIndex) VALUES (1, 'e1', 0)")
            execSQL("INSERT INTO workout_sessions (id, workoutId, workoutNameSnapshot, startedAt, finishedAt, durationSeconds) VALUES (10, 1, 'Push Day', 2000, 3000, 1000)")
            execSQL("INSERT INTO set_logs (id, sessionId, exerciseId, exerciseNameSnapshot, setNumber, reps, weightKg, completedAt) VALUES (100, 10, 'e1', 'Bench Press', 1, 10, 50.0, 2500)")
            close()
        }

        // 2. Run migration 1 to 2
        db = helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2)

        // 3. Verify all existing v1 data survived
        val exCursor = db.query("SELECT id, name FROM exercises WHERE id = 'e1'")
        assertTrue(exCursor.moveToFirst())
        assertEquals("Bench Press", exCursor.getString(1))
        exCursor.close()

        val workoutCursor = db.query("SELECT id, name FROM workouts WHERE id = 1")
        assertTrue(workoutCursor.moveToFirst())
        assertEquals("Push Day", workoutCursor.getString(1))
        workoutCursor.close()

        val sessionCursor = db.query("SELECT id, workoutNameSnapshot FROM workout_sessions WHERE id = 10")
        assertTrue(sessionCursor.moveToFirst())
        assertEquals("Push Day", sessionCursor.getString(1))
        sessionCursor.close()

        val setCursor = db.query("SELECT id, reps, weightKg FROM set_logs WHERE id = 100")
        assertTrue(setCursor.moveToFirst())
        assertEquals(10, setCursor.getInt(1))
        assertEquals(50.0, setCursor.getDouble(2), 0.001)
        setCursor.close()

        // 4. Verify favorite_exercises table is created and operational
        db.execSQL("INSERT INTO favorite_exercises (exerciseId, createdAt) VALUES ('e1', 5000)")
        val favCursor = db.query("SELECT exerciseId, createdAt FROM favorite_exercises WHERE exerciseId = 'e1'")
        assertTrue(favCursor.moveToFirst())
        assertEquals("e1", favCursor.getString(0))
        assertEquals(5000L, favCursor.getLong(1))
        favCursor.close()
    }
}
