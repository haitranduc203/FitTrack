package com.haitranduc.fittrack.data.local.seed

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.haitranduc.fittrack.core.database.FitTrackDatabase
import com.haitranduc.fittrack.data.local.dao.ExerciseDao
import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.domain.repository.SeedImportError
import com.haitranduc.fittrack.domain.repository.SeedImportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.IOException

fun interface AssetSeedReader {
    @Throws(IOException::class)
    fun readSeedJson(assetFileName: String): String
}

class AndroidAssetSeedReader(private val context: Context) : AssetSeedReader {
    override fun readSeedJson(assetFileName: String): String {
        return context.assets.open(assetFileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}

class ExerciseSeedImporter(
    private val database: FitTrackDatabase,
    private val exerciseDao: ExerciseDao,
    private val assetReader: AssetSeedReader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun ensureSeeded(assetFileName: String = "exercises_seed.json"): SeedImportResult {
        return withContext(ioDispatcher) {
            val initialCount = try {
                exerciseDao.countExercises()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SQLiteException) {
                return@withContext SeedImportResult.Failure(SeedImportError.Database(e))
            } catch (e: Exception) {
                return@withContext SeedImportResult.Failure(SeedImportError.Unknown(e))
            }

            if (initialCount > 0) {
                return@withContext SeedImportResult.AlreadySeeded(initialCount)
            }

            val jsonString = try {
                assetReader.readSeedJson(assetFileName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FileNotFoundException) {
                return@withContext SeedImportResult.Failure(SeedImportError.FileMissing)
            } catch (e: IOException) {
                // Retry once on IO failure
                try {
                    assetReader.readSeedJson(assetFileName)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (fnf: FileNotFoundException) {
                    return@withContext SeedImportResult.Failure(SeedImportError.FileMissing)
                } catch (retryError: IOException) {
                    return@withContext SeedImportResult.Failure(SeedImportError.Unknown(retryError))
                } catch (e: Exception) {
                    return@withContext SeedImportResult.Failure(SeedImportError.Unknown(e))
                }
            } catch (e: Exception) {
                return@withContext SeedImportResult.Failure(SeedImportError.Unknown(e))
            }

            val dtos: List<SeedExerciseDto> = try {
                Json.decodeFromString<List<SeedExerciseDto>>(jsonString)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SerializationException) {
                return@withContext SeedImportResult.Failure(SeedImportError.InvalidData(e.message ?: "Malformed JSON"))
            } catch (e: IllegalArgumentException) {
                return@withContext SeedImportResult.Failure(SeedImportError.InvalidData(e.message ?: "Invalid JSON"))
            } catch (e: Exception) {
                return@withContext SeedImportResult.Failure(SeedImportError.Unknown(e))
            }

            if (dtos.isEmpty()) {
                return@withContext SeedImportResult.Failure(SeedImportError.InvalidData("Seed file is empty"))
            }

            val seenIds = HashSet<String>(dtos.size)
            for (dto in dtos) {
                if (!dto.isValid()) {
                    return@withContext SeedImportResult.Failure(
                        SeedImportError.InvalidData("Invalid fields in exercise record: ${dto.id}")
                    )
                }
                if (!seenIds.add(dto.id)) {
                    return@withContext SeedImportResult.Failure(
                        SeedImportError.InvalidData("Duplicate exercise id: ${dto.id}")
                    )
                }
            }

            val entities = dtos.map { dto ->
                ExerciseEntity(
                    id = dto.id,
                    name = dto.name,
                    bodyPart = dto.bodyPart,
                    equipment = dto.equipment,
                    target = dto.target,
                    muscleGroup = dto.muscleGroup,
                    secondaryMuscles = dto.secondaryMuscles,
                    instructions = dto.instructions,
                    imagePath = null,
                    gifPath = null
                )
            }

            try {
                database.withTransaction {
                    val countInside = exerciseDao.countExercises()
                    if (countInside > 0) {
                        return@withTransaction SeedImportResult.AlreadySeeded(countInside)
                    }
                    exerciseDao.insertAll(entities)
                    SeedImportResult.Imported(entities.size)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SQLiteException) {
                SeedImportResult.Failure(SeedImportError.Database(e))
            } catch (e: Exception) {
                SeedImportResult.Failure(SeedImportError.Unknown(e))
            }
        }
    }
}
