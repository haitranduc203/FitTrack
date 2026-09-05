package com.haitranduc.fittrack.domain.repository

import com.haitranduc.fittrack.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface ThemePreferenceRepository {
    fun observeThemePreference(): Flow<DataResult<ThemePreference>>
    suspend fun setThemePreference(preference: ThemePreference): DataResult<Unit>
}
