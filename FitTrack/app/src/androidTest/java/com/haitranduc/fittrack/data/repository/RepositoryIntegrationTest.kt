package com.haitranduc.fittrack.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.data.local.dao.ExerciseDao
import com.haitranduc.fittrack.data.local.dao.SetLogDao
import com.haitranduc.fittrack.data.local.dao.WorkoutDao
import com.haitranduc.fittrack.data.local.dao.WorkoutSessionDao
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity
import com.haitranduc.fittrack.data.local.relation.WorkoutExerciseRow
import com.haitranduc.fittrack.data.local.relation.WorkoutSessionWithSets
import com.haitranduc.fittrack.data.local.relation.WorkoutWithExercises
import com.haitranduc.fittrack.data.local.seed.AndroidAssetSeedReader
import com.haitranduc.fittrack.data.local.seed.ExerciseSeedImporter
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import com.haitranduc.fittrack.domain.repository.SeedImportResult
import com.haitranduc.fittrack.domain.repository.WorkoutHistoryRepository
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class RepositoryIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: FitTrackDatabase
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var historyRepository: WorkoutHistoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, FitTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val seedImporter = ExerciseSeedImporter(
            database = database,
            exerciseDao = database.exerciseDao(),
            assetReader = AndroidAssetSeedReader(context)
        )

        exerciseRepository = ExerciseRepositoryImpl(
            exerciseDao = database.exerciseDao(),
            seedImporter = seedImporter
        )

        workoutRepository = WorkoutRepositoryImpl(
            workoutDao = database.workoutDao()
        )

        historyRepository = WorkoutHistoryRepositoryImpl(
            workoutSessionDao = database.workoutSessionDao(),
            setLogDao = database.setLogDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==========================================
    // EXERCISE REPOSITORY TESTS
    // ==========================================

    @Test
    fun exerciseRepository_seedSuccess_andSuccessList() = runTest {
        val seedResult = exerciseRepository.ensureSeeded()
        assertTrue(seedResult is SeedImportResult.Imported)

        val listResult = exerciseRepository.observeExercises().first()
        assertTrue(listResult is DataResult.Success)
        val list = (listResult as DataResult.Success).data
        assertEquals(1324, list.size)
    }

    @Test
    fun exerciseRepository_emptyResult() = runTest {
        val exerciseResult = exerciseRepository.observeExercise("non_existent_id").first()
        assertTrue(exerciseResult is DataResult.Success)
        assertNull((exerciseResult as DataResult.Success).data)
    }

    @Test
    fun exerciseRepository_searchFilter() = runTest {
        exerciseRepository.ensureSeeded()
        val result = exerciseRepository.observeExercises(query = "Bench").first()
        assertTrue(result is DataResult.Success)
        val list = (result as DataResult.Success).data
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.name.contains("Bench", ignoreCase = true) })
    }

    @Test
    fun exerciseRepository_bodyFilter() = runTest {
        exerciseRepository.ensureSeeded()
        val result = exerciseRepository.observeExercises(bodyPart = "Chest").first()
        assertTrue(result is DataResult.Success)
        val list = (result as DataResult.Success).data
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.bodyPart.equals("Chest", ignoreCase = true) })
    }

    @Test
    fun exerciseRepository_equipmentFilter() = runTest {
        exerciseRepository.ensureSeeded()
        val result = exerciseRepository.observeExercises(equipment = "Barbell").first()
        assertTrue(result is DataResult.Success)
        val list = (result as DataResult.Success).data
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.equipment.equals("Barbell", ignoreCase = true) })
    }

    @Test
    fun exerciseRepository_combinedFilter() = runTest {
        exerciseRepository.ensureSeeded()
        val result = exerciseRepository.observeExercises(query = "Press", bodyPart = "Chest", equipment = "Barbell").first()
        assertTrue(result is DataResult.Success)
        val list = (result as DataResult.Success).data
        assertTrue(list.isNotEmpty())
        assertTrue(list.all {
            it.name.contains("Press", ignoreCase = true) &&
                    it.bodyPart.equals("Chest", ignoreCase = true) &&
                    it.equipment.equals("Barbell", ignoreCase = true)
        })
    }

    @Test
    fun exerciseRepository_daoFailureMapping_returnsDatabaseError() = runTest {
        val failingDao = object : ExerciseDao {
            override fun observeAll(): Flow<List<ExerciseEntity>> = flow { throw SQLiteException("Disk failure") }
            override fun observeFiltered(query: String, bodyPart: String?, equipment: String?): Flow<List<ExerciseEntity>> =
                flow { throw SQLiteException("Disk failure") }
            override fun observeById(id: String): Flow<ExerciseEntity?> =
                flow { throw SQLiteException("Disk failure") }
            override suspend fun insertAll(items: List<ExerciseEntity>): List<Long> = emptyList()
            override suspend fun countExercises(): Int = 0
        }

        val repo = ExerciseRepositoryImpl(
            exerciseDao = failingDao,
            seedImporter = ExerciseSeedImporter(database, failingDao, AndroidAssetSeedReader(context))
        )

        val result = repo.observeExercises().first()
        assertTrue(result is DataResult.Failure)
        val error = (result as DataResult.Failure).error
        assertTrue(error is DataError.Database)
    }

    // ==========================================
    // WORKOUT REPOSITORY TESTS
    // ==========================================

    @Test
    fun workoutRepository_saveUpdateDelete_andEmptyResult() = runTest {
        exerciseRepository.ensureSeeded()
        val exercises = (exerciseRepository.observeExercises().first() as DataResult.Success).data
        val ex1 = exercises[0]
        val ex2 = exercises[1]

        // 1. Save
        val workout = Workout(id = 0L, name = "Template A", createdAt = 1000L, updatedAt = 1000L, exercises = listOf(ex1, ex2))
        val saveResult = workoutRepository.save(workout)
        assertTrue(saveResult is DataResult.Success)
        val id = (saveResult as DataResult.Success).data
        assertTrue(id > 0)

        // 2. Update
        val updatedWorkout = Workout(id = id, name = "Template A Updated", createdAt = 1000L, updatedAt = 2000L, exercises = listOf(ex1))
        val updateResult = workoutRepository.save(updatedWorkout)
        assertTrue(updateResult is DataResult.Success)
        val observed = workoutRepository.observeWorkout(id).first()
        assertTrue(observed is DataResult.Success)
        val fetched = (observed as DataResult.Success).data
        assertEquals("Template A Updated", fetched?.name)
        assertEquals(1, fetched?.exercises?.size)

        // 3. Delete
        val deleteResult = workoutRepository.delete(id)
        assertTrue(deleteResult is DataResult.Success)

        // 4. Empty result
        val emptyResult = workoutRepository.observeWorkout(id).first()
        assertTrue(emptyResult is DataResult.Success)
        assertNull((emptyResult as DataResult.Success).data)
    }

    @Test
    fun workoutRepository_reorder_preservesExerciseOrder() = runTest {
        exerciseRepository.ensureSeeded()
        val exercises = (exerciseRepository.observeExercises().first() as DataResult.Success).data
        val ex1 = exercises[0]
        val ex2 = exercises[1]

        // Save with ex2 first, then ex1
        val workout = Workout(id = 0L, name = "Reordered", createdAt = 1000L, updatedAt = 1000L, exercises = listOf(ex2, ex1))
        val saveResult = workoutRepository.save(workout)
        val id = (saveResult as DataResult.Success).data

        val fetched = (workoutRepository.observeWorkout(id).first() as DataResult.Success).data
        assertNotNull(fetched)
        assertEquals(2, fetched?.exercises?.size)
        assertEquals(ex2.id, fetched?.exercises?.get(0)?.id)
        assertEquals(ex1.id, fetched?.exercises?.get(1)?.id)
    }

    @Test
    fun workoutRepository_databaseFailureMapping_returnsDatabaseError() = runTest {
        val failingDao = object : WorkoutDao(database) {
            override fun observeWorkouts(): Flow<List<WorkoutEntity>> = flow { throw SQLiteException("DB error") }
            override fun observeWorkoutById(id: Long): Flow<WorkoutEntity?> = flow { throw SQLiteException("DB error") }
            override fun observeWorkoutExercises(workoutId: Long): Flow<List<WorkoutExerciseRow>> = flow { throw SQLiteException("DB error") }
            override suspend fun insertWorkout(workout: WorkoutEntity): Long = throw SQLiteException("DB error")
            override suspend fun updateWorkout(workout: WorkoutEntity): Int = throw SQLiteException("DB error")
            override suspend fun deleteWorkout(workoutId: Long): Int = throw SQLiteException("DB error")
            override suspend fun insertWorkoutExercises(items: List<com.haitranduc.fittrack.data.local.entity.WorkoutExerciseEntity>): List<Long> = throw SQLiteException("DB error")
            override suspend fun deleteWorkoutExercises(workoutId: Long): Int = throw SQLiteException("DB error")
            override suspend fun deleteWorkoutExercise(workoutId: Long, exerciseId: String): Int = throw SQLiteException("DB error")
        }

        val repo = WorkoutRepositoryImpl(workoutDao = failingDao)
        val observeResult = repo.observeWorkouts().first()
        assertTrue(observeResult is DataResult.Failure)
        assertTrue((observeResult as DataResult.Failure).error is DataError.Database)

        val saveResult = repo.save(Workout(0L, "Fail", 0L, 0L, emptyList()))
        assertTrue(saveResult is DataResult.Failure)
        assertTrue((saveResult as DataResult.Failure).error is DataError.Database)

        val deleteResult = repo.delete(1L)
        assertTrue(deleteResult is DataResult.Failure)
        assertTrue((deleteResult as DataResult.Failure).error is DataError.Database)
    }

    // ==========================================
    // HISTORY REPOSITORY TESTS
    // ==========================================

    @Test
    fun historyRepository_persistencePrimitives_andSessionDetail() = runTest {
        // 1. Insert session
        val session = WorkoutSession(0L, null, "Leg Day", 1000L, 2000L, 1000L, emptyList())
        val insertResult = historyRepository.insertSession(session)
        assertTrue(insertResult is DataResult.Success)
        val sessionId = (insertResult as DataResult.Success).data

        // 2. Insert set
        val setLog = SetLog(0L, sessionId, "ex_squat", "Squat", 1, 10, 100.0, 1500L)
        val setInsertResult = historyRepository.insertSet(setLog)
        assertTrue(setInsertResult is DataResult.Success)

        // 3. Update session
        val updateResult = historyRepository.updateSession(session.copy(id = sessionId, workoutNameSnapshot = "Leg Day Heavy"))
        assertTrue(updateResult is DataResult.Success)

        // 4. Session detail
        val detailResult = historyRepository.observeSession(sessionId).first()
        assertTrue(detailResult is DataResult.Success)
        val detail = (detailResult as DataResult.Success).data
        assertNotNull(detail)
        assertEquals("Leg Day Heavy", detail?.workoutNameSnapshot)
        assertEquals(1, detail?.sets?.size)
        assertEquals("Squat", detail?.sets?.get(0)?.exerciseNameSnapshot)

        // 5. Delete session
        val deleteResult = historyRepository.deleteSession(sessionId)
        assertTrue(deleteResult is DataResult.Success)

        val afterDelete = historyRepository.observeSession(sessionId).first()
        assertTrue(afterDelete is DataResult.Success)
        assertNull((afterDelete as DataResult.Success).data)
    }

    @Test
    fun historyRepository_finishedOnlyHistory_ordersNewestFirst() = runTest {
        val s1 = WorkoutSession(0L, null, "S1", 100L, 500L, 400L, emptyList())
        val s2 = WorkoutSession(0L, null, "S2", 600L, 1200L, 600L, emptyList())
        val sUnfinished = WorkoutSession(0L, null, "Active", 1300L, null, null, emptyList())

        val id1 = (historyRepository.insertSession(s1) as DataResult.Success).data
        val id2 = (historyRepository.insertSession(s2) as DataResult.Success).data
        historyRepository.insertSession(sUnfinished)

        val historyResult = historyRepository.observeHistory().first()
        assertTrue(historyResult is DataResult.Success)
        val list = (historyResult as DataResult.Success).data
        assertEquals(2, list.size)
        assertEquals(id2, list[0].id)
        assertEquals(id1, list[1].id)
    }

    @Test
    fun historyRepository_databaseFailureMapping_returnsDatabaseError() = runTest {
        val failingSessionDao = object : WorkoutSessionDao {
            override suspend fun insertSession(session: WorkoutSessionEntity): Long = throw SQLiteException("DB error")
            override suspend fun updateSession(session: WorkoutSessionEntity): Int = throw SQLiteException("DB error")
            override suspend fun deleteSession(sessionId: Long): Int = throw SQLiteException("DB error")
            override fun observeSessionById(id: Long): Flow<WorkoutSessionEntity?> = flow { throw SQLiteException("DB error") }
            override suspend fun getActiveSession(): WorkoutSessionEntity? = throw SQLiteException("DB error")
            override fun observeActiveSession(): Flow<WorkoutSessionEntity?> = flow { throw SQLiteException("DB error") }
            override fun observeFinishedSessions(): Flow<List<WorkoutSessionEntity>> = flow { throw SQLiteException("DB error") }
            override fun observeSetsForSession(sessionId: Long): Flow<List<SetLogEntity>> = flow { throw SQLiteException("DB error") }
        }

        val failingSetLogDao = object : SetLogDao {
            override suspend fun insertSetLog(setLog: SetLogEntity): Long = throw SQLiteException("DB error")
            override suspend fun deleteSetLog(id: Long): Int = throw SQLiteException("DB error")
            override fun observeSetLogsForSession(sessionId: Long): Flow<List<SetLogEntity>> = flow { throw SQLiteException("DB error") }
        }

        val repo = WorkoutHistoryRepositoryImpl(failingSessionDao, failingSetLogDao)

        val historyResult = repo.observeHistory().first()
        assertTrue(historyResult is DataResult.Failure)
        assertTrue((historyResult as DataResult.Failure).error is DataError.Database)

        val insertSessionResult = repo.insertSession(WorkoutSession(0L, null, "S", 0L, null, null, emptyList()))
        assertTrue(insertSessionResult is DataResult.Failure)
        assertTrue((insertSessionResult as DataResult.Failure).error is DataError.Database)

        val insertSetResult = repo.insertSet(SetLog(0L, 1L, "e", "E", 1, 10, 10.0, 0L))
        assertTrue(insertSetResult is DataResult.Failure)
        assertTrue((insertSetResult as DataResult.Failure).error is DataError.Database)

        val deleteResult = repo.deleteSession(1L)
        assertTrue(deleteResult is DataResult.Failure)
        assertTrue((deleteResult as DataResult.Failure).error is DataError.Database)
    }

    @Test
    fun historyRepository_emptyResult_returnsSuccessWithEmptyList() = runTest {
        val result = historyRepository.observeHistory().first()
        assertTrue("Expected DataResult.Success on empty history, got: $result", result is DataResult.Success)
        val data = (result as DataResult.Success).data
        assertNotNull(data)
        assertTrue("Expected emptyList() for empty history, got: $data", data.isEmpty())
    }

    // ==========================================
    // CANCELLATION & FATAL ERROR TESTS
    // ==========================================

    @Test
    fun cancellation_isRethrown_notMappedToDataResultFailure() = runTest {
        val cancellingDao = object : ExerciseDao {
            override fun observeAll(): Flow<List<ExerciseEntity>> = flow { throw CancellationException("Flow cancelled") }
            override fun observeFiltered(query: String, bodyPart: String?, equipment: String?): Flow<List<ExerciseEntity>> =
                flow { throw CancellationException("Flow cancelled") }
            override fun observeById(id: String): Flow<ExerciseEntity?> =
                flow { throw CancellationException("Flow cancelled") }
            override suspend fun insertAll(items: List<ExerciseEntity>): List<Long> = emptyList()
            override suspend fun countExercises(): Int = 0
        }

        val repo = ExerciseRepositoryImpl(
            exerciseDao = cancellingDao,
            seedImporter = ExerciseSeedImporter(database, cancellingDao, AndroidAssetSeedReader(context))
        )

        var caughtCancellation = false
        try {
            repo.observeExercises().first()
        } catch (e: CancellationException) {
            caughtCancellation = true
        }
        assertTrue("CancellationException must be rethrown, not converted to DataResult.Failure", caughtCancellation)
    }

    @Test(expected = LinkageError::class)
    fun exerciseRepository_fatalError_isRethrown_notMappedToFailure() = runTest {
        val failingDao = object : ExerciseDao {
            override fun observeAll(): Flow<List<ExerciseEntity>> = flow { throw LinkageError("Fatal LinkageError in ExerciseDao") }
            override fun observeFiltered(query: String, bodyPart: String?, equipment: String?): Flow<List<ExerciseEntity>> =
                flow { throw LinkageError("Fatal LinkageError in ExerciseDao") }
            override fun observeById(id: String): Flow<ExerciseEntity?> =
                flow { throw LinkageError("Fatal LinkageError in ExerciseDao") }
            override suspend fun insertAll(items: List<ExerciseEntity>): List<Long> = emptyList()
            override suspend fun countExercises(): Int = 0
        }

        val repo = ExerciseRepositoryImpl(
            exerciseDao = failingDao,
            seedImporter = ExerciseSeedImporter(database, failingDao, AndroidAssetSeedReader(context))
        )

        repo.observeExercises().first()
    }

    @Test(expected = LinkageError::class)
    fun workoutRepository_fatalError_isRethrown_notMappedToFailure() = runTest {
        val failingDao = object : WorkoutDao(database) {
            override fun observeWorkouts(): Flow<List<WorkoutEntity>> = flow { throw LinkageError("Fatal LinkageError in WorkoutDao") }
            override fun observeWorkoutById(id: Long): Flow<WorkoutEntity?> = flow { throw LinkageError("Fatal LinkageError in WorkoutDao") }
            override fun observeWorkoutExercises(workoutId: Long): Flow<List<WorkoutExerciseRow>> = flow { throw LinkageError("Fatal LinkageError in WorkoutDao") }
            override suspend fun insertWorkout(workout: WorkoutEntity): Long = 0L
            override suspend fun updateWorkout(workout: WorkoutEntity): Int = 0
            override suspend fun deleteWorkout(workoutId: Long): Int = 0
            override suspend fun insertWorkoutExercises(items: List<com.haitranduc.fittrack.data.local.entity.WorkoutExerciseEntity>): List<Long> = emptyList()
            override suspend fun deleteWorkoutExercises(workoutId: Long): Int = 0
            override suspend fun deleteWorkoutExercise(workoutId: Long, exerciseId: String): Int = 0
        }

        val repo = WorkoutRepositoryImpl(workoutDao = failingDao)
        repo.observeWorkouts().first()
    }

    @Test(expected = LinkageError::class)
    fun workoutHistoryRepository_fatalError_isRethrown_notMappedToFailure() = runTest {
        val failingSessionDao = object : WorkoutSessionDao {
            override suspend fun insertSession(session: WorkoutSessionEntity): Long = 0L
            override suspend fun updateSession(session: WorkoutSessionEntity): Int = 0
            override suspend fun deleteSession(sessionId: Long): Int = 0
            override fun observeSessionById(id: Long): Flow<WorkoutSessionEntity?> = flow { throw LinkageError("Fatal LinkageError in WorkoutSessionDao") }
            override suspend fun getActiveSession(): WorkoutSessionEntity? = null
            override fun observeActiveSession(): Flow<WorkoutSessionEntity?> = flow { throw LinkageError("Fatal LinkageError in WorkoutSessionDao") }
            override fun observeFinishedSessions(): Flow<List<WorkoutSessionEntity>> = flow { throw LinkageError("Fatal LinkageError in WorkoutSessionDao") }
            override fun observeSetsForSession(sessionId: Long): Flow<List<SetLogEntity>> = flow { throw LinkageError("Fatal LinkageError in WorkoutSessionDao") }
        }

        val failingSetLogDao = object : SetLogDao {
            override suspend fun insertSetLog(setLog: SetLogEntity): Long = 0L
            override suspend fun deleteSetLog(id: Long): Int = 0
            override fun observeSetLogsForSession(sessionId: Long): Flow<List<SetLogEntity>> = flow { throw LinkageError("Fatal LinkageError in SetLogDao") }
        }

        val repo = WorkoutHistoryRepositoryImpl(failingSessionDao, failingSetLogDao)
        repo.observeHistory().first()
    }
}
