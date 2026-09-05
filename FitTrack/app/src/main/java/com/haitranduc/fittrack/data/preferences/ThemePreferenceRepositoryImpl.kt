package com.haitranduc.fittrack.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ThemePreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferenceRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemePreferenceRepository {

    companion object {
        val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
    }

    override fun observeThemePreference(): Flow<DataResult<ThemePreference>> {
        return dataStore.data
            .map<Preferences, DataResult<ThemePreference>> { preferences ->
                val rawName = preferences[THEME_PREFERENCE_KEY]
                val preference = if (rawName.isNullOrBlank()) {
                    ThemePreference.SYSTEM
                } else {
                    try {
                        ThemePreference.valueOf(rawName)
                    } catch (e: IllegalArgumentException) {
                        ThemePreference.SYSTEM
                    }
                }
                DataResult.Success(preference)
            }
            .catch { exception ->
                if (exception is kotlinx.coroutines.CancellationException) throw exception
                if (exception is java.lang.Error) throw exception
                if (exception is IOException) {
                    emit(DataResult.Failure(DataError.Database(exception)))
                } else {
                    emit(DataResult.Failure(DataError.Unknown(exception)))
                }
            }
    }

    override suspend fun setThemePreference(preference: ThemePreference): DataResult<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[THEME_PREFERENCE_KEY] = preference.name
            }
            DataResult.Success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: java.lang.Error) {
            throw e
        } catch (e: IOException) {
            DataResult.Failure(DataError.Database(e))
        } catch (e: Exception) {
            DataResult.Failure(DataError.Unknown(e))
        }
    }
}
