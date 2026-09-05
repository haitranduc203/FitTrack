package com.haitranduc.fittrack.presentation.settings

import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.testing.FakeThemePreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeThemePreferenceRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeThemePreferenceRepository()
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoadsSystemPreference() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(ThemePreference.SYSTEM, viewModel.uiState.value.selectedTheme)
        assertFalse(viewModel.uiState.value.isUpdatingTheme)
        assertNull(viewModel.uiState.value.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun onThemeSelected_updatesPreferenceSuccessfully() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onThemeSelected(ThemePreference.DARK)
        advanceUntilIdle()

        assertEquals(ThemePreference.DARK, viewModel.uiState.value.selectedTheme)
        assertFalse(viewModel.uiState.value.isUpdatingTheme)
        assertEquals(1, repository.setPreferenceCallCount)

        collectJob.cancel()
    }

    @Test
    fun onThemeSelected_samePreference_isNoOp() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onThemeSelected(ThemePreference.SYSTEM)
        advanceUntilIdle()

        assertEquals(0, repository.setPreferenceCallCount)

        collectJob.cancel()
    }

    @Test
    fun onThemeSelected_repositoryFailure_surfacesError() = runTest(testDispatcher) {
        repository.setError = DataError.Database(RuntimeException("Write error"))
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onThemeSelected(ThemePreference.LIGHT)
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isUpdatingTheme)
        assertEquals(ThemePreference.SYSTEM, viewModel.uiState.value.selectedTheme)

        viewModel.onClearError()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun observeThemePreference_readFailure_surfacesError_andRetryRecovers() = runTest(testDispatcher) {
        repository.observeError = DataError.Database(RuntimeException("Read error"))
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        // Clear error and trigger retry
        repository.observeError = null
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(ThemePreference.SYSTEM, viewModel.uiState.value.selectedTheme)

        collectJob.cancel()
    }

    @Test
    fun onThemeSelected_failedWrite_retainsPreviouslySelectedTheme() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // First select DARK successfully
        viewModel.onThemeSelected(ThemePreference.DARK)
        advanceUntilIdle()
        assertEquals(ThemePreference.DARK, viewModel.uiState.value.selectedTheme)

        // Now attempt LIGHT which fails
        repository.setError = DataError.Database(RuntimeException("Write error"))
        viewModel.onThemeSelected(ThemePreference.LIGHT)
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), viewModel.uiState.value.errorMessage)
        assertEquals(ThemePreference.DARK, viewModel.uiState.value.selectedTheme)

        collectJob.cancel()
    }

    @Test
    fun onThemeSelected_writeFailure_emitsOneShotSnackbar_withSingleConsumptionAndNoReplay() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val events = mutableListOf<SettingsEvent>()
        val eventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        repository.setError = DataError.Database(RuntimeException("Disk write error"))
        viewModel.onThemeSelected(ThemePreference.DARK)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(
            SettingsEvent.ShowSnackbar(UiText.StringResource(R.string.error_database)),
            events[0]
        )

        // Cancel first collector; subsequent collector should see no replay
        eventsJob.cancel()

        val replayedEvents = mutableListOf<SettingsEvent>()
        val replayJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(replayedEvents)
        }
        advanceUntilIdle()
        assertTrue(replayedEvents.isEmpty())

        replayJob.cancel()
        collectJob.cancel()
    }
}
