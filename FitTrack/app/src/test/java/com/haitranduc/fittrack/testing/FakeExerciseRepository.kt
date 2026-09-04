package com.haitranduc.fittrack.testing

import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.ExerciseRepository
import com.haitranduc.fittrack.domain.repository.SeedImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeExerciseRepository : ExerciseRepository {

    var seedResult: SeedImportResult = SeedImportResult.Imported(1324)
    var ensureSeededCallCount = 0

    val exercisesFlow = MutableStateFlow<List<Exercise>>(emptyList())
    var returnDataFailure: Boolean = false

    override suspend fun ensureSeeded(): SeedImportResult {
        ensureSeededCallCount++
        return seedResult
    }

    override fun observeExercises(
        query: String,
        bodyPart: String?,
        equipment: String?
    ): Flow<DataResult<List<Exercise>>> {
        return exercisesFlow.map { list ->
            if (returnDataFailure) {
                DataResult.Failure(DataError.Database(RuntimeException("Simulated database failure")))
            } else {
                val filtered = list.filter { exercise ->
                    val matchesQuery = query.isBlank() ||
                            exercise.name.contains(query, ignoreCase = true) ||
                            exercise.target.contains(query, ignoreCase = true)
                    val matchesBodyPart = bodyPart == null || exercise.bodyPart.equals(bodyPart, ignoreCase = true)
                    val matchesEquipment = equipment == null || exercise.equipment.equals(equipment, ignoreCase = true)
                    matchesQuery && matchesBodyPart && matchesEquipment
                }
                DataResult.Success(filtered)
            }
        }
    }

    override fun observeExercise(id: String): Flow<DataResult<Exercise?>> {
        return exercisesFlow.map { list ->
            if (returnDataFailure) {
                DataResult.Failure(DataError.Database(RuntimeException("Simulated database failure")))
            } else {
                DataResult.Success(list.find { it.id == id })
            }
        }
    }
}
