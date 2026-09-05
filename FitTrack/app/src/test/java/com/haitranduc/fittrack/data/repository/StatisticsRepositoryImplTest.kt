package com.haitranduc.fittrack.data.repository

import com.haitranduc.fittrack.data.local.dao.StatisticsDao
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeStatisticsDao
    private lateinit var repository: StatisticsRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeStatisticsDao()
        repository = StatisticsRepositoryImpl(dao)
    }

    @Test
    fun observeTotalWorkouts_success_emitsCount() = runTest(testDispatcher) {
        dao.workoutsFlow.value = 5L
        val result = repository.observeTotalWorkouts().first()
        assertTrue(result is DataResult.Success)
        assertEquals(5L, (result as DataResult.Success).data)
    }

    @Test
    fun observeTotalWorkouts_daoFailure_mapsToDataResultFailure() = runTest(testDispatcher) {
        dao.shouldThrow = IOException("SQL read error")
        val result = repository.observeTotalWorkouts().first()
        assertTrue(result is DataResult.Failure)
        assertTrue((result as DataResult.Failure).error is DataError.Database)
    }

    @Test
    fun observeTotalWorkouts_cancellationException_isRethrown() = runTest(testDispatcher) {
        dao.shouldThrow = CancellationException("Flow cancelled")
        try {
            repository.observeTotalWorkouts().first()
            fail("CancellationException must be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Flow cancelled", e.message)
        }
    }

    @Test
    fun observeTotalWorkouts_error_isRethrown() = runTest(testDispatcher) {
        dao.shouldThrow = AssertionError("Fatal assertion")
        try {
            repository.observeTotalWorkouts().first()
            fail("Error must be rethrown")
        } catch (e: AssertionError) {
            assertEquals("Fatal assertion", e.message)
        }
    }

    @Test
    fun observeTotalWorkouts_reactiveUpdates_emitsNewValues() = runTest(testDispatcher) {
        dao.workoutsFlow.value = 1L
        assertEquals(1L, (repository.observeTotalWorkouts().first() as DataResult.Success).data)

        dao.workoutsFlow.value = 2L
        assertEquals(2L, (repository.observeTotalWorkouts().first() as DataResult.Success).data)
    }

    @Test
    fun observeTotalCompletedSets_successAndFailureMapping() = runTest(testDispatcher) {
        dao.setsFlow.value = 10L
        val success = repository.observeTotalCompletedSets().first()
        assertEquals(10L, (success as DataResult.Success).data)

        dao.shouldThrow = RuntimeException("DB error")
        val failure = repository.observeTotalCompletedSets().first()
        assertTrue(failure is DataResult.Failure)
    }

    @Test
    fun observeTotalCompletedSets_cancellationAndError_isRethrown() = runTest(testDispatcher) {
        dao.shouldThrow = CancellationException("Cancelled")
        try {
            repository.observeTotalCompletedSets().first()
            fail("CancellationException must be rethrown")
        } catch (e: CancellationException) {
            // Expected
        }

        dao.shouldThrow = AssertionError("Fatal")
        try {
            repository.observeTotalCompletedSets().first()
            fail("Error must be rethrown")
        } catch (e: AssertionError) {
            // Expected
        }
    }

    @Test
    fun observeTotalTrainingTime_successAndFailureMapping() = runTest(testDispatcher) {
        dao.timeFlow.value = 3600L
        val success = repository.observeTotalTrainingTimeSeconds().first()
        assertEquals(3600L, (success as DataResult.Success).data)

        dao.shouldThrow = RuntimeException("DB error")
        val failure = repository.observeTotalTrainingTimeSeconds().first()
        assertTrue(failure is DataResult.Failure)
    }

    @Test
    fun observeTotalTrainingTime_cancellationAndError_isRethrown() = runTest(testDispatcher) {
        dao.shouldThrow = CancellationException("Cancelled")
        try {
            repository.observeTotalTrainingTimeSeconds().first()
            fail("CancellationException must be rethrown")
        } catch (e: CancellationException) {
            // Expected
        }

        dao.shouldThrow = AssertionError("Fatal")
        try {
            repository.observeTotalTrainingTimeSeconds().first()
            fail("Error must be rethrown")
        } catch (e: AssertionError) {
            // Expected
        }
    }

    private class FakeStatisticsDao : StatisticsDao {
        val workoutsFlow = MutableStateFlow(0L)
        val setsFlow = MutableStateFlow(0L)
        val timeFlow = MutableStateFlow(0L)
        var shouldThrow: Throwable? = null

        override fun observeTotalWorkouts(): Flow<Long> = flow {
            val ex = shouldThrow
            if (ex != null) throw ex
            workoutsFlow.collect { emit(it) }
        }

        override fun observeTotalCompletedSets(): Flow<Long> = flow {
            val ex = shouldThrow
            if (ex != null) throw ex
            setsFlow.collect { emit(it) }
        }

        override fun observeTotalTrainingTimeSeconds(): Flow<Long> = flow {
            val ex = shouldThrow
            if (ex != null) throw ex
            timeFlow.collect { emit(it) }
        }
    }
}
