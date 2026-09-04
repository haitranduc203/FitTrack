package com.haitranduc.fittrack.testing

import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWorkoutRepository : WorkoutRepository {

    private val workoutsFlow = MutableStateFlow<List<Workout>>(emptyList())
    var saveError: DataError? = null
    var deleteError: DataError? = null
    var observeError: DataError? = null
    var savedWorkouts = mutableListOf<Workout>()
    private var nextId = 1L

    fun setWorkouts(workouts: List<Workout>) {
        workoutsFlow.value = workouts
    }

    override fun observeWorkouts(): Flow<DataResult<List<Workout>>> {
        return workoutsFlow.map { list ->
            observeError?.let { DataResult.Failure(it) } ?: DataResult.Success(list)
        }
    }

    override fun observeWorkout(id: Long): Flow<DataResult<Workout?>> {
        return workoutsFlow.map { list ->
            observeError?.let { DataResult.Failure(it) } ?: DataResult.Success(list.find { it.id == id })
        }
    }

    override suspend fun save(workout: Workout): DataResult<Long> {
        saveError?.let { return DataResult.Failure(it) }
        val id = if (workout.id == 0L) nextId++ else workout.id
        val updatedWorkout = workout.copy(id = id)
        savedWorkouts.add(updatedWorkout)
        val currentList = workoutsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index >= 0) {
            currentList[index] = updatedWorkout
        } else {
            currentList.add(updatedWorkout)
        }
        workoutsFlow.value = currentList
        return DataResult.Success(id)
    }

    override suspend fun delete(workoutId: Long): DataResult<Unit> {
        deleteError?.let { return DataResult.Failure(it) }
        workoutsFlow.value = workoutsFlow.value.filterNot { it.id == workoutId }
        return DataResult.Success(Unit)
    }
}
