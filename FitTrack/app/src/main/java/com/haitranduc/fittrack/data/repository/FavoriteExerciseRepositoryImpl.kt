package com.haitranduc.fittrack.data.repository

import com.haitranduc.fittrack.data.local.dao.FavoriteExerciseDao
import com.haitranduc.fittrack.data.local.entity.FavoriteExerciseEntity
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.FavoriteExerciseRepository
import com.haitranduc.fittrack.domain.time.TimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoriteExerciseRepositoryImpl @Inject constructor(
    private val favoriteExerciseDao: FavoriteExerciseDao,
    private val timeProvider: TimeProvider
) : FavoriteExerciseRepository {

    override fun observeFavoriteIds(): Flow<DataResult<Set<String>>> {
        return favoriteExerciseDao.observeFavoriteExerciseIds()
            .map<List<String>, DataResult<Set<String>>> { ids ->
                DataResult.Success(ids.toSet())
            }
            .catch { throwable ->
                if (throwable is CancellationException) throw throwable
                if (throwable is Error) throw throwable
                emit(DataResult.Failure(DataError.Database(throwable)))
            }
    }

    override suspend fun setFavorite(exerciseId: String, isFavorite: Boolean): DataResult<Unit> {
        return try {
            if (isFavorite) {
                favoriteExerciseDao.insertFavorite(
                    FavoriteExerciseEntity(
                        exerciseId = exerciseId,
                        createdAt = timeProvider.currentTimeMillis()
                    )
                )
            } else {
                favoriteExerciseDao.deleteFavorite(exerciseId)
            }
            DataResult.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Error) {
            throw error
        } catch (t: Throwable) {
            DataResult.Failure(DataError.Database(t))
        }
    }
}
