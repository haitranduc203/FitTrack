package com.haitranduc.fittrack.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.haitranduc.fittrack.data.preferences.ThemePreferenceRepositoryImpl
import com.haitranduc.fittrack.domain.repository.ThemePreferenceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindThemePreferenceRepository(
        impl: ThemePreferenceRepositoryImpl
    ): ThemePreferenceRepository

    companion object {
        private const val SETTINGS_PREFERENCES_FILE = "fittrack_settings"

        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile(SETTINGS_PREFERENCES_FILE) }
            )
        }
    }
}
