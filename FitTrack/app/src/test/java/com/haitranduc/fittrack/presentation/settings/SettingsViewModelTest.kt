package com.haitranduc.fittrack.presentation.settings

import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.testing.FakeThemePreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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
}
