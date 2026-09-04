package com.haitranduc.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haitranduc.fittrack.data.local.entity.FavoriteExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface FavoriteExerciseDao {

    @Query("SELECT exerciseId FROM favorite_exercises ORDER BY createdAt DESC")
    fun observeFavoriteExerciseIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_exercises WHERE exerciseId = :exerciseId)")
    fun observeIsFavorite(exerciseId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteExerciseEntity): Long

    @Query("DELETE FROM favorite_exercises WHERE exerciseId = :exerciseId")
    suspend fun deleteFavorite(exerciseId: String): Int
}
