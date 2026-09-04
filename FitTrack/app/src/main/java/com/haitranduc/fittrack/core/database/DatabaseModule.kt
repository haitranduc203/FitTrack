package com.haitranduc.fittrack.core.database

import android.content.Context
import androidx.room.Room
import com.haitranduc.fittrack.data.local.dao.ExerciseDao
import com.haitranduc.fittrack.data.local.dao.FavoriteExerciseDao
import com.haitranduc.fittrack.data.local.dao.SetLogDao
import com.haitranduc.fittrack.data.local.dao.WorkoutDao
import com.haitranduc.fittrack.data.local.dao.WorkoutSessionDao
import com.haitranduc.fittrack.data.local.seed.AndroidAssetSeedReader
import com.haitranduc.fittrack.data.local.seed.AssetSeedReader
import com.haitranduc.fittrack.data.local.seed.ExerciseSeedImporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitTrackDatabase {
        return Room.databaseBuilder(
            context,
            FitTrackDatabase::class.java,
            "fittrack.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideFavoriteExerciseDao(database: FitTrackDatabase): FavoriteExerciseDao {
        return database.favoriteExerciseDao()
    }

    @Provides
    @Singleton
    fun provideExerciseDao(database: FitTrackDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(database: FitTrackDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutSessionDao(database: FitTrackDatabase): WorkoutSessionDao {
        return database.workoutSessionDao()
    }

    @Provides
    @Singleton
    fun provideSetLogDao(database: FitTrackDatabase): SetLogDao {
        return database.setLogDao()
    }

    @Provides
    @Singleton
    fun provideAssetSeedReader(@ApplicationContext context: Context): AssetSeedReader {
        return AndroidAssetSeedReader(context)
    }

    @Provides
    @Singleton
    fun provideExerciseSeedImporter(
        database: FitTrackDatabase,
        exerciseDao: ExerciseDao,
        assetReader: AssetSeedReader
    ): ExerciseSeedImporter {
        return ExerciseSeedImporter(
            database = database,
            exerciseDao = exerciseDao,
            assetReader = assetReader
        )
    }
}
