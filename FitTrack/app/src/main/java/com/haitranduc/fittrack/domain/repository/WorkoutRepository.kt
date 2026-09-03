package com.haitranduc.fittrack.domain.repository

import com.haitranduc.fittrack.domain.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeWorkouts(): Flow<DataResult<List<Workout>>>
    fun observeWorkout(id: Long): Flow<DataResult<Workout?>>
    suspend fun save(workout: Workout): DataResult<Long>
    suspend fun delete(workoutId: Long): DataResult<Unit>
}
