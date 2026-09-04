package com.haitranduc.fittrack.data.repository

import android.database.sqlite.SQLiteException
import com.haitranduc.fittrack.data.local.dao.WorkoutDao
import com.haitranduc.fittrack.data.local.entity.WorkoutEntity
import com.haitranduc.fittrack.data.local.relation.WorkoutWithExercises
import com.haitranduc.fittrack.data.mapper.toDomain
import com.haitranduc.fittrack.data.mapper.toEntity
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override fun observeWorkouts(): Flow<DataResult<List<Workout>>> {
        return workoutDao.observeWorkouts()
            .map<List<WorkoutEntity>, DataResult<List<Workout>>> { workoutEntities ->
                val workouts = workoutEntities.map { entity ->
                    Workout(
                        id = entity.id,
                        name = entity.name,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        exercises = emptyList()
                    )
                }
                DataResult.Success(workouts)
            }
            .catch { e ->
                e.rethrowIfCancellationOrFatal()
                emit(
                    if (e is SQLiteException) DataResult.Failure(DataError.Database(e))
                    else DataResult.Failure(DataError.Unknown(e))
                )
            }
    }

    override fun observeWorkout(id: Long): Flow<DataResult<Workout?>> {
        return workoutDao.observeWorkoutWithExercises(id)
            .map<WorkoutWithExercises?, DataResult<Workout?>> { relation ->
                DataResult.Success(relation?.toDomain())
            }
            .catch { e ->
                e.rethrowIfCancellationOrFatal()
                emit(
                    if (e is SQLiteException) DataResult.Failure(DataError.Database(e))
                    else DataResult.Failure(DataError.Unknown(e))
                )
            }
    }

    override suspend fun save(workout: Workout): DataResult<Long> {
        return try {
            val exerciseIds = workout.exercises.map { it.id }
            val savedId = workoutDao.saveWorkout(workout.toEntity(), exerciseIds)
            DataResult.Success(savedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            DataResult.Failure(DataError.Database(e))
        } catch (e: Exception) {
            DataResult.Failure(DataError.Unknown(e))
        }
    }

    override suspend fun delete(workoutId: Long): DataResult<Unit> {
        return try {
            workoutDao.deleteWorkout(workoutId)
            DataResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            DataResult.Failure(DataError.Database(e))
        } catch (e: Exception) {
            DataResult.Failure(DataError.Unknown(e))
        }
    }
}
