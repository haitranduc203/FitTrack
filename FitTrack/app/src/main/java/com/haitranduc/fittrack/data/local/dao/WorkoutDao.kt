package com.haitranduc.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.withTransaction
import com.haitranduc.fittrack.data.local.entity.WorkoutEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutExerciseEntity
import com.haitranduc.fittrack.data.local.relation.WorkoutExerciseRow
import com.haitranduc.fittrack.data.local.relation.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Dao
@JvmSuppressWildcards
abstract class WorkoutDao(protected val db: RoomDatabase) {

    @Query("SELECT * FROM workouts ORDER BY updatedAt DESC, id DESC")
    abstract fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    abstract fun observeWorkoutById(id: Long): Flow<WorkoutEntity?>

    @Query("""
        SELECT exercises.*, workout_exercises.orderIndex
        FROM workout_exercises
        INNER JOIN exercises ON exercises.id = workout_exercises.exerciseId
        WHERE workout_exercises.workoutId = :workoutId
        ORDER BY workout_exercises.orderIndex ASC
    """)
    abstract fun observeWorkoutExercises(workoutId: Long): Flow<List<WorkoutExerciseRow>>

    open fun observeWorkoutWithExercises(workoutId: Long): Flow<WorkoutWithExercises?> {
        return combine(observeWorkoutById(workoutId), observeWorkoutExercises(workoutId)) { workout, exercises ->
            workout?.let { WorkoutWithExercises(it, exercises) }
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    abstract suspend fun updateWorkout(workout: WorkoutEntity): Int

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    abstract suspend fun deleteWorkout(workoutId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertWorkoutExercises(items: List<WorkoutExerciseEntity>): List<Long>

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    abstract suspend fun deleteWorkoutExercises(workoutId: Long): Int

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    abstract suspend fun deleteWorkoutExercise(workoutId: Long, exerciseId: String): Int

    open suspend fun saveWorkout(workout: WorkoutEntity, exerciseIds: List<String>): Long {
        return db.withTransaction {
            val actualId = if (workout.id == 0L) {
                insertWorkout(workout)
            } else {
                val updated = updateWorkout(workout)
                if (updated == 0) {
                    throw IllegalArgumentException("Workout with id ${workout.id} not found to update")
                }
                workout.id
            }
            deleteWorkoutExercises(actualId)
            if (exerciseIds.isNotEmpty()) {
                val junctionRows = exerciseIds.mapIndexed { index, exId ->
                    WorkoutExerciseEntity(workoutId = actualId, exerciseId = exId, orderIndex = index)
                }
                insertWorkoutExercises(junctionRows)
            }
            actualId
        }
    }
}
