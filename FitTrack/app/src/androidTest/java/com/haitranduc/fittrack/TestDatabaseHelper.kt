package com.haitranduc.fittrack

import android.content.Context
import androidx.room.Room
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.core.database.MIGRATION_1_2

object TestDatabaseProvider {
    @Volatile
    private var instance: FitTrackDatabase? = null

    fun getDatabase(context: Context): FitTrackDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FitTrackDatabase::class.java,
                "fittrack.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}

fun getTestDatabase(context: Context): FitTrackDatabase {
    return TestDatabaseProvider.getDatabase(context)
}
