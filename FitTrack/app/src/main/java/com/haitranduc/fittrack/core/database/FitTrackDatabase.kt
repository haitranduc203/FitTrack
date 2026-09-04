package com.haitranduc.fittrack.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.haitranduc.fittrack.data.local.dao.ExerciseDao
import com.haitranduc.fittrack.data.local.dao.FavoriteExerciseDao
import com.haitranduc.fittrack.data.local.dao.SetLogDao
import com.haitranduc.fittrack.data.local.dao.WorkoutDao
import com.haitranduc.fittrack.data.local.dao.WorkoutSessionDao
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.data.local.entity.FavoriteExerciseEntity
import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutExerciseEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        FavoriteExerciseEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(StringListConverters::class)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun favoriteExerciseDao(): FavoriteExerciseDao
}
