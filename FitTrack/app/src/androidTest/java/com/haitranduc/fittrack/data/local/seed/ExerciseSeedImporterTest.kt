package com.haitranduc.fittrack.data.local.seed

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.data.local.dao.ExerciseDao
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.domain.repository.SeedImportError
import com.haitranduc.fittrack.domain.repository.SeedImportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileNotFoundException
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ExerciseSeedImporterTest {

    private lateinit var context: Context
    private lateinit var database: FitTrackDatabase

    private val validJson = """
        [
          {
            "id": "ex_01",
            "name": "Bench Press",
            "bodyPart": "Chest",
            "equipment": "Barbell",
            "target": "Pecs",
            "muscleGroup": "Chest",
            "secondaryMuscles": ["Triceps"],
            "instructions": ["Step 1", "Step 2"]
          }
        ]
    """.trimIndent()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, FitTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun realAssetImport_importsExactly1324Records() = runTest {
        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = AndroidAssetSeedReader(context)
        )

        val result = importer.ensureSeeded()
        assertTrue(result is SeedImportResult.Imported)
        assertEquals(1324, (result as SeedImportResult.Imported).count)
        assertEquals(1324, database.exerciseDao().countExercises())
    }

    @Test
    fun secondCall_returnsAlreadySeededWithoutReimport() = runTest {
        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = AndroidAssetSeedReader(context)
        )

        val firstResult = importer.ensureSeeded()
        assertTrue(firstResult is SeedImportResult.Imported)

        val secondResult = importer.ensureSeeded()
        assertTrue(secondResult is SeedImportResult.AlreadySeeded)
        assertEquals(1324, (secondResult as SeedImportResult.AlreadySeeded).existingCount)
        assertEquals(1324, database.exerciseDao().countExercises())
    }

    @Test
    fun prepopulatedTable_avoidsReadingAsset() = runTest {
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity(
                    id = "pre_01",
                    name = "Pre-existing",
                    bodyPart = "Chest",
                    equipment = "Barbell",
                    target = "Pecs",
                    muscleGroup = "Chest",
                    secondaryMuscles = emptyList(),
                    instructions = listOf("Step 1")
                )
            )
        )

        val throwingReader = AssetSeedReader {
            throw AssertionError("Asset reader should not have been called")
        }

        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = throwingReader
        )

        val result = importer.ensureSeeded()
        assertTrue(result is SeedImportResult.AlreadySeeded)
        assertEquals(1, (result as SeedImportResult.AlreadySeeded).existingCount)
    }

    @Test
    fun missingFile_returnsFileMissingFailure() = runTest {
        val reader = AssetSeedReader { throw FileNotFoundException("File not found") }
        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = reader
        )

        val result = importer.ensureSeeded()
        assertTrue(result is SeedImportResult.Failure)
        assertTrue((result as SeedImportResult.Failure).error is SeedImportError.FileMissing)
        assertEquals(0, database.exerciseDao().countExercises())
    }

    @Test
    fun malformedJson_returnsInvalidDataFailure() = runTest {
        val reader = AssetSeedReader { "{ not valid json ]" }
        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = reader
        )

        val result = importer.ensureSeeded()
        assertTrue(result is SeedImportResult.Failure)
        assertTrue((result as SeedImportResult.Failure).error is SeedImportError.InvalidData)
        assertEquals(0, database.exerciseDao().countExercises())
    }

    @Test
    fun duplicateIds_returnsInvalidDataFailureAndLeavesTableEmpty() = runTest {
        val duplicateJson = """
            [
              {
                "id": "dup_01",
                "name": "First",
                "bodyPart": "Chest",
                "equipment": "Barbell",
                "target": "Pecs",
                "muscleGroup": "Chest",
                "secondaryMuscles": [],
                "instructions": ["Step 1"]
              },
              {
                "id": "dup_01",
                "name": "Second",
                "bodyPart": "Chest",
                "equipment": "Barbell",
                "target": "Pecs",
                "muscleGroup": "Chest",
                "secondaryMuscles": [],
                "instructions": ["Step 1"]
              }
            ]
        """.trimIndent()

        val reader = AssetSeedReader { duplicateJson }
        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = reader
        )

        val result = importer.ensureSeeded()
        assertTrue(result is SeedImportResult.Failure)
        assertTrue((result as SeedImportResult.Failure).error is SeedImportError.InvalidData)
        assertEquals(0, database.exerciseDao().countExercises())
    }

    @Test
    fun firstIOException_thenRetrySucceeds() = runTest {
        var attempts = 0
        val reader = AssetSeedReader {
            attempts++
            if (attempts == 1) {
                throw IOException("Temporary IO error")
            } else {
                validJson
            }
        }

        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = reader
        )

        val result = importer.ensureSeeded()
        assertEquals(2, attempts)
        assertTrue(result is SeedImportResult.Imported)
        assertEquals(1, (result as SeedImportResult.Imported).count)
        assertEquals(1, database.exerciseDao().countExercises())
    }

    @Test
    fun exhaustedIOException_returnsUnknownFailure() = runTest {
        var attempts = 0
        val reader = AssetSeedReader {
            attempts++
            throw IOException("Persistent IO failure attempt $attempts")
        }

        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = reader
        )

        val result = importer.ensureSeeded()
        assertEquals(2, attempts)
        assertTrue(result is SeedImportResult.Failure)
        val error = (result as SeedImportResult.Failure).error
        assertTrue("Error must be SeedImportError.Unknown, but was: $error", error is SeedImportError.Unknown)
        assertEquals(0, database.exerciseDao().countExercises())
    }

    @Test
    fun initialCountExercisesFailure_mapsToDatabaseFailure() = runTest {
        val failingDao = object : ExerciseDao {
            override fun observeAll(): Flow<List<ExerciseEntity>> = flowOf(emptyList())
            override fun observeFiltered(query: String, bodyPart: String?, equipment: String?): Flow<List<ExerciseEntity>> = flowOf(emptyList())
            override fun observeById(id: String): Flow<ExerciseEntity?> = flowOf(null)
            override suspend fun insertAll(items: List<ExerciseEntity>): List<Long> = emptyList()
            override suspend fun countExercises(): Int = throw SQLiteException("Disk I/O error on count")
        }

        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = failingDao,
            assetReader = AssetSeedReader { validJson }
        )

        val result = importer.ensureSeeded()
        assertTrue(result is SeedImportResult.Failure)
        val error = (result as SeedImportResult.Failure).error
        assertTrue("Error must be SeedImportError.Database, but was: $error", error is SeedImportError.Database)
    }

    @Test
    fun realRollback_delegatingDaoInsertsOneRowThenThrows_leavesDatabaseEmpty() = runTest {
        val realDao = database.exerciseDao()
        val delegatingDao = object : ExerciseDao by realDao {
            override suspend fun insertAll(items: List<ExerciseEntity>): List<Long> {
                // Write at least 1 row inside the active transaction
                realDao.insertAll(items.take(1))
                assertEquals(1, realDao.countExercises())
                // Throw to force transaction rollback
                throw SQLiteException("Constraint failure during bulk insert")
            }
        }

        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = delegatingDao,
            assetReader = AssetSeedReader { validJson }
        )

        val result = importer.ensureSeeded()
        assertTrue(result is SeedImportResult.Failure)
        val error = (result as SeedImportResult.Failure).error
        assertTrue("Error must be SeedImportError.Database, but was: $error", error is SeedImportError.Database)
        // Verify real DAO is empty because withTransaction rolled back the write
        assertEquals(0, realDao.countExercises())
    }

    @Test(expected = LinkageError::class)
    fun fatalError_isNotCaughtOrMappedToFailure() = runTest {
        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = AssetSeedReader { throw LinkageError("Fatal JVM error") }
        )
        importer.ensureSeeded()
    }

    @Test
    fun blankFields_returnInvalidData() = runTest {
        val blankIdJson = validJson.replace("\"id\": \"ex_01\"", "\"id\": \"   \"")
        val blankNameJson = validJson.replace("\"name\": \"Bench Press\"", "\"name\": \" \"")
        val blankBodyPartJson = validJson.replace("\"bodyPart\": \"Chest\"", "\"bodyPart\": \"\"")
        val blankEquipmentJson = validJson.replace("\"equipment\": \"Barbell\"", "\"equipment\": \" \"")
        val blankTargetJson = validJson.replace("\"target\": \"Pecs\"", "\"target\": \"\"")
        val blankMuscleGroupJson = validJson.replace("\"muscleGroup\": \"Chest\"", "\"muscleGroup\": \" \"")

        listOf(blankIdJson, blankNameJson, blankBodyPartJson, blankEquipmentJson, blankTargetJson, blankMuscleGroupJson).forEach { json ->
            val importer = ExerciseSeedImporter(
                database = database,
                exerciseDao = database.exerciseDao(),
                assetReader = AssetSeedReader { json }
            )
            val result = importer.ensureSeeded()
            assertTrue("Expected InvalidData for json: $json", result is SeedImportResult.Failure)
            assertTrue((result as SeedImportResult.Failure).error is SeedImportError.InvalidData)
        }
        assertEquals(0, database.exerciseDao().countExercises())
    }

    @Test
    fun invalidBlankSecondaryMuscle_returnsInvalidData() = runTest {
        val blankSecondaryJson = validJson.replace("\"secondaryMuscles\": [\"Triceps\"]", "\"secondaryMuscles\": [\" \"]")
        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = AssetSeedReader { blankSecondaryJson }
        )
        val result = importer.ensureSeeded()
        assertTrue(result is SeedImportResult.Failure)
        assertTrue((result as SeedImportResult.Failure).error is SeedImportError.InvalidData)
        assertEquals(0, database.exerciseDao().countExercises())
    }

    @Test
    fun emptyOrBlankInstruction_returnsInvalidData() = runTest {
        val emptyInstructionsJson = validJson.replace("\"instructions\": [\"Step 1\", \"Step 2\"]", "\"instructions\": []")
        val blankInstructionsJson = validJson.replace("\"instructions\": [\"Step 1\", \"Step 2\"]", "\"instructions\": [\"Step 1\", \" \"]")

        listOf(emptyInstructionsJson, blankInstructionsJson).forEach { json ->
            val importer = ExerciseSeedImporter(
                database = database,
                exerciseDao = database.exerciseDao(),
                assetReader = AssetSeedReader { json }
            )
            val result = importer.ensureSeeded()
            assertTrue(result is SeedImportResult.Failure)
            assertTrue((result as SeedImportResult.Failure).error is SeedImportError.InvalidData)
        }
        assertEquals(0, database.exerciseDao().countExercises())
    }

    @Test
    fun cancellation_isRethrown() = runTest {
        val importer = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = AssetSeedReader {
                throw CancellationException("Cancelled during read")
            }
        )

        var caughtCancellation = false
        try {
            importer.ensureSeeded()
        } catch (e: CancellationException) {
            caughtCancellation = true
        }
        assertTrue(caughtCancellation)
    }
}
