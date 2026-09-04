package com.haitranduc.fittrack.presentation.startup

import com.haitranduc.fittrack.domain.repository.SeedImportError
import com.haitranduc.fittrack.domain.repository.SeedImportResult
import com.haitranduc.fittrack.testing.FakeExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FitTrackViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeExerciseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeExerciseRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_seedImported_reachesReadyState() = runTest(testDispatcher) {
        repository.seedResult = SeedImportResult.Imported(1324)
        val viewModel = FitTrackViewModel(repository)

        assertEquals(StartupUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
        assertEquals(1, repository.ensureSeededCallCount)
    }

    @Test
    fun init_alreadySeeded_reachesReadyState() = runTest(testDispatcher) {
        repository.seedResult = SeedImportResult.AlreadySeeded(1324)
        val viewModel = FitTrackViewModel(repository)

        advanceUntilIdle()

        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
        assertEquals(1, repository.ensureSeededCallCount)
    }

    @Test
    fun init_seedFailure_reachesErrorState() = runTest(testDispatcher) {
        repository.seedResult = SeedImportResult.Failure(SeedImportError.FileMissing)
        val viewModel = FitTrackViewModel(repository)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is StartupUiState.Error)
        assertEquals(1, repository.ensureSeededCallCount)
    }

    @Test
    fun retry_onFailure_invokesEnsureSeededAgain() = runTest(testDispatcher) {
        repository.seedResult = SeedImportResult.Failure(SeedImportError.FileMissing)
        val viewModel = FitTrackViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is StartupUiState.Error)
        assertEquals(1, repository.ensureSeededCallCount)

        // On retry, succeed
        repository.seedResult = SeedImportResult.Imported(1324)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
        assertEquals(2, repository.ensureSeededCallCount)
    }
}
