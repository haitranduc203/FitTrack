package com.haitranduc.fittrack.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.data.local.entity.FavoriteExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteExerciseDaoTest {

    private lateinit var database: FitTrackDatabase
    private lateinit var favoriteExerciseDao: FavoriteExerciseDao
    private lateinit var exerciseDao: ExerciseDao

    private val sampleExercise1 = ExerciseEntity(
        id = "e1",
        name = "Barbell Bench Press",
        bodyPart = "chest",
        equipment = "barbell",
        target = "pectorals",
        muscleGroup = "chest",
        secondaryMuscles = listOf("triceps"),
        instructions = listOf("Push"),
        imagePath = null,
        gifPath = null
    )

    private val sampleExercise2 = ExerciseEntity(
        id = "e2",
        name = "Incline Dumbbell Press",
        bodyPart = "chest",
        equipment = "dumbbell",
        target = "pectorals",
        muscleGroup = "chest",
        secondaryMuscles = listOf("triceps"),
        instructions = listOf("Push"),
        imagePath = null,
        gifPath = null
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FitTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favoriteExerciseDao = database.favoriteExerciseDao()
        exerciseDao = database.exerciseDao()

        runBlocking {
            exerciseDao.insertAll(listOf(sampleExercise1, sampleExercise2))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveFavorites_returnsOrderedByCreatedAtDesc() = runBlocking {
        favoriteExerciseDao.insertFavorite(FavoriteExerciseEntity("e1", createdAt = 1000L))
        favoriteExerciseDao.insertFavorite(FavoriteExerciseEntity("e2", createdAt = 2000L))

        val favorites = favoriteExerciseDao.observeFavoriteExerciseIds().first()
        assertEquals(listOf("e2", "e1"), favorites)
    }

    @Test
    fun observeIsFavorite_returnsTrueWhenPresentAndFalseWhenDeleted() = runBlocking {
        assertFalse(favoriteExerciseDao.observeIsFavorite("e1").first())

        favoriteExerciseDao.insertFavorite(FavoriteExerciseEntity("e1", createdAt = 1000L))
        assertTrue(favoriteExerciseDao.observeIsFavorite("e1").first())

        favoriteExerciseDao.deleteFavorite("e1")
        assertFalse(favoriteExerciseDao.observeIsFavorite("e1").first())
    }

    @Test
    fun deleteExercise_cascadesToFavoriteExercises() = runBlocking {
        favoriteExerciseDao.insertFavorite(FavoriteExerciseEntity("e1", createdAt = 1000L))
        assertTrue(favoriteExerciseDao.observeIsFavorite("e1").first())

        // Delete parent exercise row via SQLite
        database.openHelper.writableDatabase.execSQL("DELETE FROM exercises WHERE id = 'e1'")

        assertFalse(favoriteExerciseDao.observeIsFavorite("e1").first())
        assertTrue(favoriteExerciseDao.observeFavoriteExerciseIds().first().isEmpty())
    }
}
