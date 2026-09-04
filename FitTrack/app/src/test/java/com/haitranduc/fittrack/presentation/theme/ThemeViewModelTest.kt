package com.haitranduc.fittrack.presentation.theme

import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.domain.repository.DataError
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeThemePreferenceRepository
    private lateinit var viewModel: ThemeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeThemePreferenceRepository()
        viewModel = ThemeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun themePreference_initiallyEmitsSystem() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themePreference.collect()
        }
        advanceUntilIdle()

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)

        collectJob.cancel()
    }

    @Test
    fun themePreference_updatesWhenRepositoryEmitsNewPreference() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themePreference.collect()
        }
        advanceUntilIdle()

        repository.setThemePreference(ThemePreference.DARK)
        advanceUntilIdle()

        assertEquals(ThemePreference.DARK, viewModel.themePreference.value)

        collectJob.cancel()
    }

    @Test
    fun themePreference_repositoryError_fallsBackToSystem() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themePreference.collect()
        }
        advanceUntilIdle()

        repository.observeError = DataError.Database(RuntimeException("Read error"))
        repository.setThemePreference(ThemePreference.DARK)
        advanceUntilIdle()

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)

        collectJob.cancel()
    }
}
