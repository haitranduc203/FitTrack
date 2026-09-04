package com.haitranduc.fittrack.testing

import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ThemePreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeThemePreferenceRepository(
    initialPreference: ThemePreference = ThemePreference.SYSTEM
) : ThemePreferenceRepository {

    private val preferenceFlow = MutableStateFlow(initialPreference)
    var observeError: DataError? = null
    var setError: DataError? = null
    var setPreferenceCallCount = 0
        private set

    override fun observeThemePreference(): Flow<DataResult<ThemePreference>> {
        return preferenceFlow.asStateFlow().map { pref ->
            val err = observeError
            if (err != null) {
                DataResult.Failure(err)
            } else {
                DataResult.Success(pref)
            }
        }
    }

    override suspend fun setThemePreference(preference: ThemePreference): DataResult<Unit> {
        setPreferenceCallCount++
        val err = setError
        return if (err != null) {
            DataResult.Failure(err)
        } else {
            preferenceFlow.value = preference
            DataResult.Success(Unit)
        }
    }
}
