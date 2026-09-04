package com.haitranduc.fittrack.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.domain.repository.DataResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ThemePreferenceRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: ThemePreferenceRepositoryImpl

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(testDispatcher)
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(tempFolder.root, "theme_test.preferences_pb") }
        )
        repository = ThemePreferenceRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun observeThemePreference_emptyDataStore_returnsSystemDefault() = runTest(testDispatcher) {
        val result = repository.observeThemePreference().first()
        assertTrue(result is DataResult.Success)
        assertEquals(ThemePreference.SYSTEM, (result as DataResult.Success).data)
    }

    @Test
    fun setThemePreference_persistsLightAndDark() = runTest(testDispatcher) {
        val setLightResult = repository.setThemePreference(ThemePreference.LIGHT)
        assertTrue(setLightResult is DataResult.Success)

        val lightResult = repository.observeThemePreference().first()
        assertEquals(ThemePreference.LIGHT, (lightResult as DataResult.Success).data)

        val setDarkResult = repository.setThemePreference(ThemePreference.DARK)
        assertTrue(setDarkResult is DataResult.Success)

        val darkResult = repository.observeThemePreference().first()
        assertEquals(ThemePreference.DARK, (darkResult as DataResult.Success).data)
    }

    @Test
    fun observeThemePreference_corruptedOrInvalidValue_fallsBackToSystem() = runTest(testDispatcher) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("theme_preference")] = "INVALID_UNKNOWN_THEME"
        }

        val result = repository.observeThemePreference().first()
        assertTrue(result is DataResult.Success)
        assertEquals(ThemePreference.SYSTEM, (result as DataResult.Success).data)
    }
}
