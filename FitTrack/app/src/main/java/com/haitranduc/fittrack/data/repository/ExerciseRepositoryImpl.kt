package com.haitranduc.fittrack.data.repository

import android.database.sqlite.SQLiteException
import com.haitranduc.fittrack.data.local.dao.ExerciseDao
import com.haitranduc.fittrack.data.local.dao.escapeLikeQuery
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.data.local.seed.ExerciseSeedImporter
import com.haitranduc.fittrack.data.mapper.toDomain
import com.haitranduc.fittrack.domain.repository.SeedImportResult
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val seedImporter: ExerciseSeedImporter
) : ExerciseRepository {

    override suspend fun ensureSeeded(): SeedImportResult {
        return seedImporter.ensureSeeded()
    }

    override fun observeExercises(
        query: String,
        bodyPart: String?,
        equipment: String?
    ): Flow<DataResult<List<Exercise>>> {
        val escapedQuery = if (query.isBlank()) "" else escapeLikeQuery(query.trim())
        return exerciseDao.observeFiltered(escapedQuery, bodyPart, equipment)
            .map<List<ExerciseEntity>, DataResult<List<Exercise>>> { entities ->
                DataResult.Success(entities.map { it.toDomain() })
            }
            .catch { e ->
                e.rethrowIfCancellationOrFatal()
                emit(
                    if (e is SQLiteException) DataResult.Failure(DataError.Database(e))
                    else DataResult.Failure(DataError.Unknown(e))
                )
            }
    }

    override fun observeExercise(id: String): Flow<DataResult<Exercise?>> {
        return exerciseDao.observeById(id)
            .map<ExerciseEntity?, DataResult<Exercise?>> { entity ->
                DataResult.Success(entity?.toDomain())
            }
            .catch { e ->
                e.rethrowIfCancellationOrFatal()
                emit(
                    if (e is SQLiteException) DataResult.Failure(DataError.Database(e))
                    else DataResult.Failure(DataError.Unknown(e))
                )
            }
    }
}
