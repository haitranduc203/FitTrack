package com.haitranduc.fittrack.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoriteExerciseRepository {
    fun observeFavoriteIds(): Flow<DataResult<Set<String>>>
    suspend fun setFavorite(exerciseId: String, isFavorite: Boolean): DataResult<Unit>
}
