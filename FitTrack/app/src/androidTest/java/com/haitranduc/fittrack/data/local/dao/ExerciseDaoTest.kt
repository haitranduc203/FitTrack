package com.haitranduc.fittrack.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var database: FitTrackDatabase
    private lateinit var exerciseDao: ExerciseDao

    private val exercise1 = ExerciseEntity(
        id = "ex_001",
        name = "Barbell Bench Press",
        bodyPart = "Chest",
        equipment = "Barbell",
        target = "Pectorals",
        muscleGroup = "Chest",
        secondaryMuscles = listOf("Triceps", "Shoulders"),
        instructions = listOf("Lie down", "Press up")
    )

    private val exercise2 = ExerciseEntity(
        id = "ex_002",
        name = "pull up",
        bodyPart = "Back",
        equipment = "Bodyweight",
        target = "Lats",
        muscleGroup = "Back",
        secondaryMuscles = listOf("Biceps"),
        instructions = listOf("Grip bar", "Pull up")
    )

    private val exercise3 = ExerciseEntity(
        id = "ex_003",
        name = "100% Extreme_Press\\Row",
        bodyPart = "Back",
        equipment = "Dumbbell",
        target = "Upper Back",
        muscleGroup = "Back",
        secondaryMuscles = emptyList(),
        instructions = listOf("Lift weight")
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FitTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exerciseDao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndCountExercises() = runTest {
        assertEquals(0, exerciseDao.countExercises())
        exerciseDao.insertAll(listOf(exercise1, exercise2))
        assertEquals(2, exerciseDao.countExercises())
    }

    @Test
    fun observeByIdReturnsCorrectExercise() = runTest {
        exerciseDao.insertAll(listOf(exercise1, exercise2))
        val result = exerciseDao.observeById("ex_001").first()
        assertNotNull(result)
        assertEquals("Barbell Bench Press", result?.name)

        val missing = exerciseDao.observeById("unknown").first()
        assertNull(missing)
    }

    @Test
    fun observeFiltered_ordersCaseInsensitivelyByName() = runTest {
        exerciseDao.insertAll(listOf(exercise2, exercise1)) // "pull up", "Barbell Bench Press"
        val items = exerciseDao.observeFiltered("", null, null).first()
        assertEquals(2, items.size)
        assertEquals("Barbell Bench Press", items[0].name)
        assertEquals("pull up", items[1].name)
    }

    @Test
    fun observeFiltered_withBodyPartFilter() = runTest {
        exerciseDao.insertAll(listOf(exercise1, exercise2))
        val chestItems = exerciseDao.observeFiltered("", "Chest", null).first()
        assertEquals(1, chestItems.size)
        assertEquals("ex_001", chestItems[0].id)

        val backItems = exerciseDao.observeFiltered("", "back", null).first()
        assertEquals(1, backItems.size)
        assertEquals("ex_002", backItems[0].id)
    }

    @Test
    fun observeFiltered_withEquipmentFilter() = runTest {
        exerciseDao.insertAll(listOf(exercise1, exercise2))
        val barbellItems = exerciseDao.observeFiltered("", null, "Barbell").first()
        assertEquals(1, barbellItems.size)
        assertEquals("ex_001", barbellItems[0].id)
    }

    @Test
    fun observeFiltered_withEscapedSearchQuery() = runTest {
        exerciseDao.insertAll(listOf(exercise1, exercise2, exercise3))
        // exercise3 has name: "100% Extreme_Press\Row"
        // Searching for "%", "_", or "\" should only match exercise3, not every exercise
        val escapedPercent = escapeLikeQuery("%")
        val percentResults = exerciseDao.observeFiltered(escapedPercent, null, null).first()
        assertEquals(1, percentResults.size)
        assertEquals("ex_003", percentResults[0].id)

        val escapedUnderscore = escapeLikeQuery("_")
        val underscoreResults = exerciseDao.observeFiltered(escapedUnderscore, null, null).first()
        assertEquals(1, underscoreResults.size)
        assertEquals("ex_003", underscoreResults[0].id)

        val escapedBackslash = escapeLikeQuery("\\")
        val backslashResults = exerciseDao.observeFiltered(escapedBackslash, null, null).first()
        assertEquals(1, backslashResults.size)
        assertEquals("ex_003", backslashResults[0].id)
    }

    @Test
    fun observeFiltered_combinedFiltersEmptyResult() = runTest {
        exerciseDao.insertAll(listOf(exercise1, exercise2))
        val result = exerciseDao.observeFiltered("Bench", "Back", "Barbell").first()
        assertTrue(result.isEmpty())
    }
}
