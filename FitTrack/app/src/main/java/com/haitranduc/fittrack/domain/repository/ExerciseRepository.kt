package com.haitranduc.fittrack.domain.repository

import com.haitranduc.fittrack.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    suspend fun ensureSeeded(): SeedImportResult
    fun observeExercises(query: String = "", bodyPart: String? = null, equipment: String? = null): Flow<DataResult<List<Exercise>>>
    fun observeExercise(id: String): Flow<DataResult<Exercise?>>
}
