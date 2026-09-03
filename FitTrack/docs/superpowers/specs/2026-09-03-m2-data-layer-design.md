# M2 Data Layer Design

## Goal

Build and verify FitTrack's local persistence boundary without connecting the M1 Compose screens to Room. M2 delivers a reproducible exercise seed, Room schema and DAOs, domain models and mappers, repositories, and Hilt bindings. The M1 UI continues to use presentation mock data until M3.

## Scope boundaries

M2 includes:

- preprocessing the local exercise dataset into a release-safe Android asset;
- Room entities, relations, type converters, DAOs, and `FitTrackDatabase` version 1;
- an idempotent seed importer;
- domain models, entity/domain mappers, repository interfaces and implementations;
- Hilt database/repository bindings;
- database, seed, mapper, and repository tests.

M2 excludes:

- ViewModels, use cases, and changes that connect Compose screens to Room;
- workout validation, start/finish rules, timer behavior, or other M3 business orchestration;
- remote APIs, Firebase, authentication, analytics, or cloud sync;
- exercise images, GIFs, media identifiers, media URLs, or media attribution fields in the seed asset.

## Commit policy

Implementation is split into eight tasks. Each task produces exactly one focused commit after its own tests pass. Do not combine tasks into a single commit, squash the task commits, or create a final catch-all implementation commit. Planning documentation may have its own `docs:` commit before implementation begins.

The implementation commits are, in order:

1. exercise seed asset and reproducible preprocessing;
2. Room entities, converters, and exported version-1 schema foundation;
3. Exercise DAO and database tests;
4. idempotent exercise seed importer;
5. workout DAO, ordered junction relation, and transaction tests;
6. workout-session and set-log DAOs with history-preservation tests;
7. domain models, mappers, repository contracts, implementations, and integration tests;
8. Hilt database/repository wiring and full M2 verification.

## Exercise seed

The source is `../exercises-dataset/data/exercises.json`, which currently contains 1,324 records. Add a standard-library-only preprocessing script at `tools/prepare_exercise_seed.py`. The script accepts explicit input and output paths so it does not depend on one developer's absolute filesystem layout.

The generated asset is `app/src/main/assets/exercises_seed.json`. Every output record contains only:

```text
id
name
bodyPart       <- source body_part
equipment
target
muscleGroup    <- source muscle_group
secondaryMuscles
instructions   <- source instruction_steps.en, stored as an ordered array
```

The generated file must contain exactly 1,324 records for the current source snapshot. IDs must be nonblank and unique. All scalar fields must be nonblank; `secondaryMuscles` may be empty; `instructions` must contain at least one nonblank English step. Records are sorted by ID to make regeneration deterministic. The script must fail with a non-zero exit code for missing input, malformed JSON, duplicate IDs, missing required fields, or invalid English instructions.

Do not copy `image`, `gif_url`, `media_id`, `attribution`, or any files from `images/` and `videos/`. Include the dataset's MIT license notice for the non-media data in `app/src/main/assets/licenses/exercises_dataset_LICENSE.txt`.

## Room schema

Database name: `fittrack.db`. Database class: `FitTrackDatabase`. Version: `1`. Schema export remains enabled and Room schemas are committed under `app/schemas` using the existing KSP configuration.

### ExerciseEntity

Table `exercises`:

```text
id: String primary key
name: String
bodyPart: String
equipment: String
target: String
muscleGroup: String
secondaryMuscles: List<String>
instructions: List<String>
imagePath: String? = null
gifPath: String? = null
```

Use a deterministic JSON-array type converter for both string lists. Add non-unique indexes for `name`, `bodyPart`, and `equipment`. Exercise rows are read-only to app users.

### WorkoutEntity

Table `workouts`:

```text
id: Long primary key auto-generated
name: String
createdAt: Long
updatedAt: Long
```

### WorkoutExerciseEntity

Table `workout_exercises`:

```text
workoutId: Long
exerciseId: String
orderIndex: Int
```

Primary key: `(workoutId, exerciseId)`. Add indexes for `workoutId`, `exerciseId`, and a unique `(workoutId, orderIndex)` index. `workoutId` references `workouts.id` with cascade delete. `exerciseId` references `exercises.id` without cascading deletion.

### WorkoutSessionEntity

Table `workout_sessions`:

```text
id: Long primary key auto-generated
workoutId: Long?
workoutNameSnapshot: String
startedAt: Long
finishedAt: Long?
durationSeconds: Long?
```

Add indexes for `workoutId` and `finishedAt`. `workoutId` references `workouts.id` with `ON DELETE SET NULL`, preserving history snapshots after template deletion.

### SetLogEntity

Table `set_logs`:

```text
id: Long primary key auto-generated
sessionId: Long
exerciseId: String
exerciseNameSnapshot: String
setNumber: Int
reps: Int
weightKg: Double
completedAt: Long
```

Add an index for `sessionId` and a unique `(sessionId, exerciseId, setNumber)` index. `sessionId` references `workout_sessions.id` with cascade delete. `exerciseId` is a snapshot reference rather than a foreign key, so completed history does not depend on the current exercise table.

Room enforces structural integrity. M3 use cases will enforce workout name length, minimum exercise count, reps/weight ranges, single-active-session orchestration, and finish rules.

## Relations and transactions

Define focused relation projections:

- `WorkoutWithExercises`: an embedded `WorkoutEntity` with exercises ordered by `WorkoutExerciseEntity.orderIndex`;
- `WorkoutSessionWithSets`: an embedded session with set logs ordered by exercise/set order needed by the repository contract.

Room's ordinary `@Relation` does not reliably express ordered many-to-many payloads. Use explicit transactional DAO queries/projections or a junction projection that preserves `orderIndex`; do not sort by exercise name or insertion accident.

Writing a workout template and replacing its exercise junction rows is one `@Transaction`. Failures must not leave a partially updated template. Deleting a workout removes junction rows, sets completed/session `workoutId` to null, and retains the session and set snapshots.

## DAO contracts

### ExerciseDao

- observe all exercises ordered case-insensitively by name;
- observe one exercise by ID;
- search by name using escaped `LIKE` input;
- filter by body part;
- filter by equipment;
- combined search/body-part/equipment query where null filters mean â€œallâ€;
- insert all with conflict strategy `ABORT`;
- count rows.

### WorkoutDao

- observe workouts ordered by most recently updated;
- observe a workout and its ordered exercises;
- insert/update/delete a workout;
- insert/replace its junction list transactionally;
- delete one workout-exercise relation;
- replace an entire ordered exercise list atomically.

The composite primary key rejects duplicate exercises. The unique order index rejects ambiguous ordering.

### WorkoutSessionDao

- insert/update a session;
- get the unfinished active session;
- observe finished sessions only, newest first;
- observe one session with ordered set logs;
- delete a session for future cancel orchestration.

### SetLogDao

- insert/delete a completed set;
- observe sets for a session in deterministic order.

DAO methods must be `suspend` for one-shot writes/reads and return `Flow` for observable reads. No DAO is exposed to presentation code.

## Seed importer

Create a `SeedExerciseDto` and JSON parser using `kotlinx.serialization-json`; adding the Kotlin serialization plugin and JSON runtime dependency is allowed because M2 now has a concrete parsing requirement. The importer reads the asset on `Dispatchers.IO`, validates DTOs before mapping, and inserts inside a Room transaction.

`ExerciseSeedImporter.ensureSeeded()` is a suspend function with these results:

```text
Imported(count)
AlreadySeeded(existingCount)
Failure(FileMissing | InvalidData | Database | Unknown)
```

Behavior:

- if `ExerciseDao.countExercises()` is greater than zero, return `AlreadySeeded` without reading or inserting;
- if empty, parse and validate the full asset, then insert all records atomically;
- retry at most once within the same call for an I/O read failure only;
- parsing, validation, and database errors are not retried blindly;
- cancellation is rethrown and never converted into `Unknown`;
- no unmanaged coroutine scope and no blocking work on Main.

M2 tests call this importer directly. App-start/UI orchestration is deferred to M3, avoiding an application-level background scope solely for seeding.

## Domain and mapping boundary

Add immutable domain models `Exercise`, `Workout`, `WorkoutSession`, and `SetLog`. Domain models contain Kotlin values only and have no Room annotations or Android resource IDs. Mapping functions are explicit, deterministic, and tested in both directions where writes occur.

Repository interfaces live in `domain/repository`; implementations live in `data/repository`. Repository methods expose domain models, `Flow`, and typed data failuresâ€”not Room entities, DAOs, or raw SQLite exceptions.

- `ExerciseRepository`: ensure seed, observe/get/search/filter exercises.
- `WorkoutRepository`: observe/get/save/update/delete workout templates with ordered exercise IDs.
- `WorkoutHistoryRepository`: observe finished history, get a session with sets, and provide persistence primitives needed by later M3 use cases.

M2 repositories persist and retrieve data but do not decide whether a workout is valid, whether a session may start/finish, or whether reps/weight are acceptable. Those are M3 use-case responsibilities.

## Dependency injection

Create Hilt modules in `core/database` or a focused `di` package inside the existing single app module:

- singleton `FitTrackDatabase` built with application context and name `fittrack.db`;
- DAO providers from the database;
- repository bindings to implementations;
- seed importer dependencies.

Do not use destructive migration fallback. Version 1 needs no migration, but the exported schema is required for future migration tests.

## Error handling

Use small sealed result/error types for seed and repository boundaries. Map expected Room/storage failures to `DataError.Database`; preserve coroutine cancellation. Do not catch `Throwable` without rethrowing cancellation and fatal JVM errors. Tests assert failure types rather than log text.

## Testing strategy

Use local JVM tests for pure seed validation, type converters, and mappers where possible. Use Android instrumentation tests with in-memory Room for DAO relations, foreign keys, transactions, seed asset import, and repository integration.

Required coverage:

- generated seed has 1,324 unique valid records and no media fields/paths;
- Exercise insert/count/get, search, body-part filter, equipment filter, combined filter, and empty result;
- seed first import, repeat import, malformed data, and atomic failure;
- Workout create/update/delete, ordered relation, duplicate exercise rejection, and transactional rollback;
- active-session query and finished-only history;
- SetLog insert/order/delete and duplicate set rejection;
- deleting a workout retains completed history and snapshots;
- mapper round trips and repository success/empty/failure behavior;
- Hilt/database graph compiles.

For each implementation task, first add a failing test, run it to confirm the expected failure, implement the minimum production code, and rerun the focused tests before committing.

## M2 completion gate

M2 passes only when:

- the eight task commits are separate and ordered;
- `exercises_seed.json` is reproducible and contains exactly 1,324 metadata/English-instruction records with no media content;
- Room schema version 1 and exported schema match this design;
- important DAO, transaction, seed, mapper, and repository tests pass;
- `:app:assembleDebug`, `:app:assembleRelease`, `:app:testDebugUnitTest`, `:app:lintDebug`, and `connectedDebugAndroidTest` pass;
- M1 navigation tests still pass and M1 Compose screens still use their mock data;
- `git diff --check` is clean and only intended files under `FitTrack/` changed.
