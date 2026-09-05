package com.haitranduc.fittrack.testing

import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.FavoriteExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeFavoriteExerciseRepository : FavoriteExerciseRepository {

    val favoriteIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    var errorToEmit: DataError? = null
    var setFavoriteError: DataError? = null
    var setFavoriteCallCount = 0

    override fun observeFavoriteIds(): Flow<DataResult<Set<String>>> {
        return favoriteIdsFlow.map { ids ->
            val err = errorToEmit
            if (err != null) {
                DataResult.Failure(err)
            } else {
                DataResult.Success(ids)
            }
        }
    }

    override suspend fun setFavorite(exerciseId: String, isFavorite: Boolean): DataResult<Unit> {
        setFavoriteCallCount++
        val err = setFavoriteError
        if (err != null) {
            return DataResult.Failure(err)
        }
        val current = favoriteIdsFlow.value
        favoriteIdsFlow.value = if (isFavorite) current + exerciseId else current - exerciseId
        return DataResult.Success(Unit)
    }
}
