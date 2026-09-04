package com.haitranduc.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("""
        SELECT * FROM exercises
        WHERE (:query = '' OR name LIKE '%' || :query || '%' ESCAPE '\' COLLATE NOCASE)
          AND (:bodyPart IS NULL OR bodyPart = :bodyPart COLLATE NOCASE)
          AND (:equipment IS NULL OR equipment = :equipment COLLATE NOCASE)
        ORDER BY name COLLATE NOCASE, id
    """)
    fun observeFiltered(query: String, bodyPart: String?, equipment: String?): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<ExerciseEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int
}

fun escapeLikeQuery(input: String): String {
    return input
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}
