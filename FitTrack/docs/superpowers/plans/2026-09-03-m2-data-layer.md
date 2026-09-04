# M2 Data Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tested local Room persistence boundary and reproducible 1,324-record exercise seed while leaving the M1 Compose UI on mock data.

**Architecture:** A preprocessing tool produces a media-free seed asset. Room DAOs are the local data source; data-layer implementations map Room entities to immutable domain models and expose typed results through domain repository contracts. M2 tests the boundary independently, and M3 will connect it to ViewModels/UI.

**Tech Stack:** Kotlin 2.3.20, AGP 9.0.1, Room 2.6.1, KSP 2.3.6, Hilt 2.59.2, Kotlin Serialization JSON, Coroutines/Flow, JUnit 4, AndroidX Test, Python 3 standard library for seed preprocessing.

**Spec:** `FitTrack/docs/superpowers/specs/2026-09-03-m2-data-layer-design.md`

## Global Constraints

- Work only in `FitTrack/` on `feature/data-layer`; do not modify the source dataset or project-plan folders.
- Keep one `:app` module, database name `fittrack.db`, Room version `1`, minSdk `24`, compileSdk/targetSdk `36`, and Java 17.
- The M1 Compose screens continue using presentation mock data; do not add ViewModels/use cases or connect UI to Room.
- Seed exactly 1,324 records containing metadata and English instruction steps only; package no image, GIF, media ID, media URL, or media attribution field.
- Room entities never cross the data boundary; repositories expose domain models and typed errors.
- Preserve coroutine cancellation and perform file/database work off Main.
- Do not use destructive migration fallback.
- Tasks 1-8 each end in exactly one focused implementation commit. Do not combine, squash, amend across task boundaries, or create a final catch-all implementation commit.

---

### Task 1: Reproducible exercise seed asset

**Files:**
- Create: `tools/prepare_exercise_seed.py`
- Create: `tools/test_prepare_exercise_seed.py`
- Create: `app/src/main/assets/exercises_seed.json`
- Create: `app/src/main/assets/licenses/exercises_dataset_LICENSE.txt`

**Interfaces:**
- Consumes source JSON path `../exercises-dataset/data/exercises.json` via CLI argument.
- Produces a deterministic JSON array with keys `id`, `name`, `bodyPart`, `equipment`, `target`, `muscleGroup`, `secondaryMuscles`, and `instructions`.

- [ ] **Step 1: Write failing preprocessing tests**

Use `unittest`, `tempfile`, and `subprocess` only. Cover a valid record, sorted deterministic output, duplicate IDs, missing `instruction_steps.en`, blank instructions, malformed JSON, and confirmation that media keys are absent. The valid assertion must be equivalent to:

```python
self.assertEqual(
    list(output[0]),
    ["id", "name", "bodyPart", "equipment", "target", "muscleGroup", "secondaryMuscles", "instructions"],
)
self.assertNotIn("image", json.dumps(output))
self.assertNotIn("gif_url", json.dumps(output))
```

- [ ] **Step 2: Run tests and confirm the expected failure**

Run from `FitTrack/`:

```powershell
python -m unittest tools/test_prepare_exercise_seed.py -v
```

Expected: FAIL because `prepare_exercise_seed.py` does not exist.

- [ ] **Step 3: Implement the standard-library preprocessing CLI**

Implement these functions and CLI contract:

```python
import argparse
import json
import pathlib
import sys

OUTPUT_KEYS = (
    "id", "name", "bodyPart", "equipment", "target",
    "muscleGroup", "secondaryMuscles", "instructions",
)

def require_text(record: dict, key: str) -> str:
    value = record.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"invalid {key}")
    return value.strip()

def transform_record(source: dict) -> dict:
    steps_map = source.get("instruction_steps")
    steps = steps_map.get("en") if isinstance(steps_map, dict) else None
    if not isinstance(steps, list) or not steps or any(not isinstance(step, str) or not step.strip() for step in steps):
        raise ValueError("invalid instruction_steps.en")
    secondary = source.get("secondary_muscles")
    if not isinstance(secondary, list) or any(not isinstance(item, str) or not item.strip() for item in secondary):
        raise ValueError("invalid secondary_muscles")
    return {
        "id": require_text(source, "id"),
        "name": require_text(source, "name"),
        "bodyPart": require_text(source, "body_part"),
        "equipment": require_text(source, "equipment"),
        "target": require_text(source, "target"),
        "muscleGroup": require_text(source, "muscle_group"),
        "secondaryMuscles": [item.strip() for item in secondary],
        "instructions": [step.strip() for step in steps],
    }

def prepare_seed(input_path: pathlib.Path, output_path: pathlib.Path) -> int:
    source = json.loads(input_path.read_text(encoding="utf-8"))
    if not isinstance(source, list):
        raise ValueError("root must be an array")
    records = [transform_record(item) for item in source]
    ids = [record["id"] for record in records]
    if len(ids) != len(set(ids)):
        raise ValueError("duplicate exercise id")
    records.sort(key=lambda item: item["id"])
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return len(records)

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    args = parser.parse_args(argv)
    try:
        count = prepare_seed(args.input, args.output)
    except (OSError, json.JSONDecodeError, KeyError, TypeError, ValueError) as error:
        print(f"seed generation failed: {error}", file=sys.stderr)
        return 1
    print(count)
    return 0
```

Append exactly:

    if __name__ == "__main__":
        raise SystemExit(main())

- [ ] **Step 4: Run preprocessing tests and generate the real asset**

```powershell
python -m unittest tools/test_prepare_exercise_seed.py -v
python tools/prepare_exercise_seed.py --input ..\exercises-dataset\data\exercises.json --output app\src\main\assets\exercises_seed.json
```

Expected: all Python tests pass and the generator reports `1324` records.

- [ ] **Step 5: Validate the generated asset independently**

Run a Python one-liner/script assertion that checks count `1324`, 1,324 unique IDs, sorted IDs, nonblank required fields, nonempty English steps, exact key set, and absence of the substrings `images/` and `videos/`. Copy the dataset `LICENSE` text into the license asset; do not copy `NOTICE.md` or its media grant.

- [ ] **Step 6: Commit only Task 1**

```powershell
git add tools app/src/main/assets
git commit -m "data: add reproducible exercise seed"
```

### Task 2: Room entities, converters, and schema configuration

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/entity/ExerciseEntity.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/entity/WorkoutEntity.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/entity/WorkoutExerciseEntity.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/entity/WorkoutSessionEntity.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/entity/SetLogEntity.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/core/database/StringListConverters.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/core/database/StringListConvertersTest.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces five Room entity types using the exact table/column/index/foreign-key design from the spec.
- Produces `StringListConverters.fromList(List<String>): String` and `StringListConverters.toList(String): List<String>`.

- [ ] **Step 1: Add failing converter tests**

Test empty lists, quotes, Unicode, embedded commas/newlines, and a round trip:

```kotlin
@Test fun list_roundTripsWithoutDelimiterLoss() {
    val original = listOf("upper back", "quote \"inside\"", "line\nstep")
    assertEquals(original, converters.toList(converters.fromList(original)))
}
```

- [ ] **Step 2: Run the focused test and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*StringListConvertersTest" --no-daemon
```

Expected: FAIL because the converter does not exist.

- [ ] **Step 3: Add Kotlin Serialization and schema export configuration**

Add the `org.jetbrains.kotlin.plugin.serialization` plugin at version `2.3.20`, `org.jetbrains.kotlinx:kotlinx-serialization-json` using a pinned catalog version, and `androidTestImplementation(libs.androidx.room.testing)`. Configure KSP:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

Do not change existing dependency versions.

- [ ] **Step 4: Implement converters and entities**

Use `Json.encodeToString`/`decodeFromString` for string lists. Define entities exactly as the spec, including:

```kotlin
@Entity(
    tableName = "workout_exercises",
    primaryKeys = ["workoutId", "exerciseId"],
    foreignKeys = [
        ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.NO_ACTION),
    ],
    indices = [Index("workoutId"), Index("exerciseId"), Index(value = ["workoutId", "orderIndex"], unique = true)],
)
data class WorkoutExerciseEntity(val workoutId: Long, val exerciseId: String, val orderIndex: Int)
```

Use `SET_NULL` for nullable `WorkoutSessionEntity.workoutId`, cascade from session to set logs, and the unique set index `(sessionId, exerciseId, setNumber)`.

- [ ] **Step 5: Run focused unit tests and compile Room processing**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*StringListConvertersTest" :app:assembleDebug --no-daemon
```

Expected: converter tests and debug compilation pass.

- [ ] **Step 6: Commit only Task 2**

```powershell
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/src/main/java/com/haitranduc/fittrack/data/local/entity app/src/main/java/com/haitranduc/fittrack/core/database/StringListConverters.kt app/src/test/java/com/haitranduc/fittrack/core/database/StringListConvertersTest.kt
git commit -m "data: define Room entities and converters"
```

### Task 3: Exercise DAO and FitTrackDatabase

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/dao/ExerciseDao.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackDatabase.kt`
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/ExerciseDaoTest.kt`
- Generate: `app/schemas/com.haitranduc.fittrack.core.database.FitTrackDatabase/1.json`

**Interfaces:**
- Produces `ExerciseDao` observable queries, `suspend fun countExercises(): Int`, and `suspend fun insertAll(items: List<ExerciseEntity>)`.
- Produces `FitTrackDatabase` version 1 containing all five entities and exposing `exerciseDao()`.

- [ ] **Step 1: Write failing in-memory DAO tests**

Create/close an in-memory database for every test and enable foreign keys. Cover insert/count, ID lookup, case-insensitive name ordering, escaped search input (`%`, `_`, `\\` treated literally), body-part filter, equipment filter, combined nullable filters, and empty results. Collect one Flow emission using `first()` under `runTest`.

- [ ] **Step 2: Run the focused instrumentation test and confirm failure**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.ExerciseDaoTest --no-daemon
```

Expected: FAIL because DAO/database types do not exist.

- [ ] **Step 3: Implement ExerciseDao**

Use deterministic queries and an explicit escaped query argument. Required combined contract:

```kotlin
@Query("""
    SELECT * FROM exercises
    WHERE (:query = '' OR name LIKE '%' || :query || '%' ESCAPE '\\' COLLATE NOCASE)
      AND (:bodyPart IS NULL OR bodyPart = :bodyPart COLLATE NOCASE)
      AND (:equipment IS NULL OR equipment = :equipment COLLATE NOCASE)
    ORDER BY name COLLATE NOCASE, id
""")
fun observeFiltered(query: String, bodyPart: String?, equipment: String?): Flow<List<ExerciseEntity>>
```

Provide a small pure escaping function at the repository/DAO boundary that transforms `\\`, `%`, and `_` in that order before passing the query.

- [ ] **Step 4: Implement FitTrackDatabase and export schema**

```kotlin
@Database(
    entities = [ExerciseEntity::class, WorkoutEntity::class, WorkoutExerciseEntity::class, WorkoutSessionEntity::class, SetLogEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(StringListConverters::class)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}
```

- [ ] **Step 5: Run focused tests and verify schema JSON exists**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.ExerciseDaoTest :app:assembleDebug --no-daemon
```

Expected: all Exercise DAO tests pass and schema `1.json` is generated.

- [ ] **Step 6: Commit only Task 3**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/data/local/dao/ExerciseDao.kt app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackDatabase.kt app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/ExerciseDaoTest.kt app/schemas
git commit -m "data: add exercise DAO and database"
```

### Task 4: Idempotent exercise seed importer

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/seed/SeedExerciseDto.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/seed/SeedImportResult.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/seed/ExerciseSeedImporter.kt`
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/data/local/seed/ExerciseSeedImporterTest.kt`

**Interfaces:**
- Produces `suspend fun ExerciseSeedImporter.ensureSeeded(): SeedImportResult`.
- Produces `Imported(count)`, `AlreadySeeded(existingCount)`, and `Failure(error)` with `FileMissing`, `InvalidData`, `Database`, or `Unknown`.

- [ ] **Step 1: Write failing seed importer tests**

Test real asset import count `1324`; second call returns `AlreadySeeded(1324)`; a prepopulated table avoids opening the asset; invalid JSON returns `InvalidData`; duplicate IDs/blank fields/empty instructions return `InvalidData`; DAO insertion failure leaves zero rows; cancellation is rethrown. Allow the importer constructor to accept an `AssetSeedReader` function/interface so failure tests do not alter production assets.

- [ ] **Step 2: Run focused instrumentation tests and confirm failure**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.seed.ExerciseSeedImporterTest --no-daemon
```

Expected: FAIL because seed importer types do not exist.

- [ ] **Step 3: Implement DTO, validation, reader, and results**

```kotlin
@Serializable
data class SeedExerciseDto(
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val muscleGroup: String,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
)

sealed interface SeedImportResult {
    data class Imported(val count: Int) : SeedImportResult
    data class AlreadySeeded(val existingCount: Int) : SeedImportResult
    data class Failure(val error: SeedImportError) : SeedImportResult
}
```

Validate the whole list before insertion. Map DTOs with `imagePath = null` and `gifPath = null`.

- [ ] **Step 4: Implement transactional, IO-safe import**

Use `withContext(Dispatchers.IO)`, `database.withTransaction`, and a recheck of count inside the transaction. Retry only an asset `IOException` once. Catch `CancellationException` first and rethrow it. Distinguish `SerializationException`/validation from `SQLiteException`; do not use an unmanaged scope.

- [ ] **Step 5: Run focused importer and Exercise DAO regression tests**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.haitranduc.fittrack.data.local --no-daemon
```

Expected: seed and Exercise DAO tests pass; row count remains 1,324 after repeated initialization.

- [ ] **Step 6: Commit only Task 4**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/data/local/seed app/src/androidTest/java/com/haitranduc/fittrack/data/local/seed
git commit -m "data: import exercise seed idempotently"
```

### Task 5: Workout persistence and ordered exercise relation

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/relation/WorkoutExerciseRow.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/relation/WorkoutWithExercises.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/dao/WorkoutDao.kt`
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/WorkoutDaoTest.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackDatabase.kt`
- Update generated: `app/schemas/com.haitranduc.fittrack.core.database.FitTrackDatabase/1.json`

**Interfaces:**
- Produces ordered `WorkoutExerciseRow(exercise: ExerciseEntity, orderIndex: Int)` and `WorkoutWithExercises(workout: WorkoutEntity, exercises: List<WorkoutExerciseRow>)`.
- Produces atomic `suspend fun saveWorkout(workout: WorkoutEntity, exerciseIds: List<String>): Long` and observable workout/relation queries.

- [ ] **Step 1: Write failing workout DAO tests**

Cover insert, update, newest-updated ordering, ordered exercises, replacement/reordering, deletion cascade to junction rows, duplicate exercise rejection, duplicate order rejection, unknown exercise foreign-key rejection, and rollback when any replacement row fails. Also verify deleting an empty/draft template works.

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.WorkoutDaoTest --no-daemon
```

Expected: FAIL because WorkoutDao does not exist.

- [ ] **Step 3: Implement ordered projections and queries**

Join `workout_exercises` to `exercises`, select `orderIndex`, and order by `orderIndex ASC`. Use `Flow<List<WorkoutExerciseRow>>` for observation and never depend on `@Relation` ordering.

- [ ] **Step 4: Implement atomic save/replace**

In an `@Transaction` DAO method, insert when `workout.id == 0L`, otherwise update and require one affected row; delete old junction rows; insert `exerciseIds.mapIndexed { index, id -> WorkoutExerciseEntity(actualId, id, index) }`; return the actual workout ID. Let constraint exceptions roll back the entire transaction.

- [ ] **Step 5: Run focused tests and schema verification**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.WorkoutDaoTest :app:assembleDebug --no-daemon
```

Expected: all workout tests pass and schema still reports version 1 with required indexes/foreign keys.

- [ ] **Step 6: Commit only Task 5**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/data/local/relation/WorkoutExerciseRow.kt app/src/main/java/com/haitranduc/fittrack/data/local/relation/WorkoutWithExercises.kt app/src/main/java/com/haitranduc/fittrack/data/local/dao/WorkoutDao.kt app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackDatabase.kt app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/WorkoutDaoTest.kt app/schemas
git commit -m "data: persist ordered workout templates"
```

### Task 6: Workout session, set log, and preserved history

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/relation/WorkoutSessionWithSets.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/dao/WorkoutSessionDao.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/dao/SetLogDao.kt`
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/WorkoutHistoryDaoTest.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackDatabase.kt`
- Update generated: `app/schemas/com.haitranduc.fittrack.core.database.FitTrackDatabase/1.json`

**Interfaces:**
- Produces `WorkoutSessionWithSets(session: WorkoutSessionEntity, sets: List<SetLogEntity>)` ordered by `exerciseId`, then `setNumber`, then `id`.
- Produces active/finished session queries and set insert/delete/observe operations.

- [ ] **Step 1: Write failing history DAO tests**

Cover active-session lookup (`finishedAt IS NULL`), finished-only history newest first, session update to finished, deterministic set order, duplicate set rejection, delete-set behavior, session cascade deleting sets, and deleting a workout template setting `workoutId` null while retaining session/name/set snapshots.

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.WorkoutHistoryDaoTest --no-daemon
```

Expected: FAIL because session/set DAOs do not exist.

- [ ] **Step 3: Implement WorkoutSessionDao and relation projection**

Use `LIMIT 1` for the unfinished active session query, `finishedAt IS NOT NULL ORDER BY finishedAt DESC, id DESC` for history, and an explicit set query for detail. Include insert/update/delete one-shot methods.

- [ ] **Step 4: Implement SetLogDao and expose DAOs from database**

Use `suspend` for insert/delete and `Flow<List<SetLogEntity>>` ordered deterministically. Add `workoutDao()`, `workoutSessionDao()`, and `setLogDao()` abstract accessors to `FitTrackDatabase`.

- [ ] **Step 5: Run all DAO tests and verify history retention**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.haitranduc.fittrack.data.local.dao --no-daemon
```

Expected: Exercise, Workout, and History DAO suites pass, including template-deletion snapshot preservation.

- [ ] **Step 6: Commit only Task 6**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/data/local/relation/WorkoutSessionWithSets.kt app/src/main/java/com/haitranduc/fittrack/data/local/dao/WorkoutSessionDao.kt app/src/main/java/com/haitranduc/fittrack/data/local/dao/SetLogDao.kt app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackDatabase.kt app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/WorkoutHistoryDaoTest.kt app/schemas
git commit -m "data: persist workout sessions and history"
```

### Task 7: Domain models, mappers, and repositories

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/model/Exercise.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/model/Workout.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/model/WorkoutSession.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/model/SetLog.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/repository/DataResult.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/repository/ExerciseRepository.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/repository/WorkoutRepository.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/repository/WorkoutHistoryRepository.kt`
- Create: focused mapper files under `app/src/main/java/com/haitranduc/fittrack/data/mapper/`
- Create: three implementation files under `app/src/main/java/com/haitranduc/fittrack/data/repository/`
- Create: mapper unit tests under `app/src/test/java/com/haitranduc/fittrack/data/mapper/`
- Create: repository integration tests under `app/src/androidTest/java/com/haitranduc/fittrack/data/repository/`

**Interfaces:**
- Produces immutable Android-free domain models.
- Produces `DataResult.Success<T>` and `DataResult.Failure(DataError)` where errors include `Database` and `Unknown`.
- Produces three domain repository contracts; no contract mentions Entity, DAO, Room, Context, or resource IDs.

- [ ] **Step 1: Write failing mapper unit tests**

Assert every field maps, ordered exercises/sets remain ordered, snapshot values survive mapping, and entity-to-domain-to-entity round trips preserve writable values. Do not test Room here.

- [ ] **Step 2: Write failing repository integration tests**

Exercise repository: seed/success/empty/search/body/equipment/combined filter and DAO failure mapping. Workout repository: save/update/delete with ordered exercises. History repository: finished-only detail and persistence primitives. Include tests that a cancelled Flow collector receives `CancellationException`, not `DataResult.Failure`.

- [ ] **Step 3: Run tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*MapperTest" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.haitranduc.fittrack.data.repository --no-daemon
```

Expected: FAIL because domain/repository types do not exist.

- [ ] **Step 4: Implement domain contracts**

Use these shapes consistently:

```kotlin
data class Exercise(val id: String, val name: String, val bodyPart: String, val equipment: String, val target: String, val muscleGroup: String, val secondaryMuscles: List<String>, val instructions: List<String>)
data class Workout(val id: Long, val name: String, val createdAt: Long, val updatedAt: Long, val exercises: List<Exercise>)
data class SetLog(val id: Long, val sessionId: Long, val exerciseId: String, val exerciseNameSnapshot: String, val setNumber: Int, val reps: Int, val weightKg: Double, val completedAt: Long)
data class WorkoutSession(val id: Long, val workoutId: Long?, val workoutNameSnapshot: String, val startedAt: Long, val finishedAt: Long?, val durationSeconds: Long?, val sets: List<SetLog>)
```

Use these exact repository signatures:

    interface ExerciseRepository {
        suspend fun ensureSeeded(): SeedImportResult
        fun observeExercises(query: String = "", bodyPart: String? = null, equipment: String? = null): Flow<DataResult<List<Exercise>>>
        fun observeExercise(id: String): Flow<DataResult<Exercise?>>
    }

    interface WorkoutRepository {
        fun observeWorkouts(): Flow<DataResult<List<Workout>>>
        fun observeWorkout(id: Long): Flow<DataResult<Workout?>>
        suspend fun save(workout: Workout): DataResult<Long>
        suspend fun delete(workoutId: Long): DataResult<Unit>
    }

    interface WorkoutHistoryRepository {
        fun observeHistory(): Flow<DataResult<List<WorkoutSession>>>
        fun observeSession(id: Long): Flow<DataResult<WorkoutSession?>>
        suspend fun insertSession(session: WorkoutSession): DataResult<Long>
        suspend fun updateSession(session: WorkoutSession): DataResult<Unit>
        suspend fun insertSet(setLog: SetLog): DataResult<Long>
        suspend fun deleteSession(sessionId: Long): DataResult<Unit>
    }

- [ ] **Step 5: Implement mappers and repository implementations**

Map all database exceptions to `DataError.Database`, unexpected recoverable exceptions to `Unknown`, and rethrow `CancellationException`. Escape search metacharacters before the DAO query. Combine workout/session base Flow and ordered child Flow without losing order. Do not perform M3 validation.

- [ ] **Step 6: Run mapper, repository, and DAO regressions**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*MapperTest" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.haitranduc.fittrack.data --no-daemon
```

Expected: all mapper/repository/data-layer instrumentation tests pass.

- [ ] **Step 7: Commit only Task 7**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/domain app/src/main/java/com/haitranduc/fittrack/data/mapper app/src/main/java/com/haitranduc/fittrack/data/repository app/src/test/java/com/haitranduc/fittrack/data/mapper app/src/androidTest/java/com/haitranduc/fittrack/data/repository
git commit -m "data: expose domain repositories"
```

### Task 8: Hilt wiring and complete M2 verification

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/core/database/DatabaseModule.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/repository/RepositoryModule.kt`
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/core/database/DatabaseContractTest.kt`

**Interfaces:**
- Produces singleton `FitTrackDatabase`, four DAO providers, `ExerciseSeedImporter`, and singleton repository bindings.
- Produces no presentation/UI dependency on repositories.

- [ ] **Step 1: Write a failing database contract test**

Open a file-backed test database without destructive fallback, assert Room version `1`, all five table names, required indexes/foreign keys from `PRAGMA` output, and the ability to resolve/use all four DAOs. Keep this test isolated from the production database file.

- [ ] **Step 2: Run contract test and confirm failure**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.core.database.DatabaseContractTest --no-daemon
```

Expected: FAIL because DI modules/final contract are incomplete.

- [ ] **Step 3: Implement Hilt modules**

Provide the production database with:

```kotlin
Room.databaseBuilder(context, FitTrackDatabase::class.java, "fittrack.db").build()
```

Use `@Module`, `@InstallIn(SingletonComponent::class)`, `@Provides`, and `@Singleton`. Bind repository interfaces to concrete implementations with `@Binds`. Do not call `ensureSeeded()` from a global coroutine or Application startup in M2.

- [ ] **Step 4: Run all required verification**

```powershell
python -m unittest tools/test_prepare_exercise_seed.py -v
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --no-daemon
.\gradlew.bat connectedDebugAndroidTest --no-daemon
git diff --check
```

Expected: all commands exit `0`; M1's eight navigation/filter tests still pass alongside all M2 tests.

- [ ] **Step 5: Audit scope and artifact contents**

Confirm `ExerciseMockData`, `WorkoutMockData`, and `HistoryMockData` are still used by M1 UI. Inspect the APK/assets to confirm `exercises_seed.json` and license exist, and no `.jpg`, `.jpeg`, `.png`, `.gif`, `images/`, or `videos/` dataset asset is packaged. Confirm the seed contains 1,324 exact-key records.

- [ ] **Step 6: Commit only Task 8**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/core/database/DatabaseModule.kt app/src/main/java/com/haitranduc/fittrack/data/repository/RepositoryModule.kt app/src/androidTest/java/com/haitranduc/fittrack/core/database/DatabaseContractTest.kt
git commit -m "data: wire M2 persistence dependencies"
```

Do not create another implementation commit after Task 8. If final verification fails, do not commit Task 8 and do not hide the failure inside a catch-all change; stop, report the failing evidence, and correct the owning task while preserving the agreed one-task-one-commit history.

## Self-review

- Spec coverage: Task 1 covers deterministic licensed seed data; Tasks 2-6 cover the complete Room schema, relations, seed, and persistence; Task 7 covers domain boundaries and repositories; Task 8 covers DI and all completion gates.
- Deferred-work scan: every required M2 behavior has an owning task; only UI integration and business orchestration are explicitly assigned to M3 by the approved spec.
- Type consistency: entity fields, DTO fields, domain models, relation projections, repository result types, and database accessors use the same names across producing and consuming tasks.
