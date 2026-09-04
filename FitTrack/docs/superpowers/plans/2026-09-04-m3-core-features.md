# M3 Core Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver FitTrack's fully persisted offline workout flow from exercise discovery through completed history.

**Architecture:** Keep Room as the source of truth behind the M2 repository contracts. Hilt ViewModels expose immutable `StateFlow` UI state to stateless Compose content, while Android-free validators/use cases own business rules and use an injected time source. Navigation passes persisted `Long` IDs, and each screen retains a dependency-free preview.

**Tech Stack:** Kotlin 2.3.20, AGP 9.0.1, Jetpack Compose Material 3, Navigation Compose 2.8.8, Lifecycle 2.10.0, Hilt 2.59.2, Room 2.6.1, Coroutines 1.10.2, JUnit 4, AndroidX Test, Java 17.

**Spec:** `docs/superpowers/specs/2026-09-04-m3-core-features-design.md`

## Global Constraints

- Work on `feature/core-features`, based on merge commit `f236514`; implementation and Git-staged changes stay inside `FitTrack/`.
- Keep one `:app` module, package `com.haitranduc.fittrack`, minSdk `24`, compileSdk/targetSdk `36`, Java 17, Room database name `fittrack.db`, and Room version `1`.
- Do not use destructive migrations, remote services, authentication, analytics, unverified media, notifications, settings, theme work, or an unapproved cancel-workout flow.
- Preserve the Clean Architecture Lite boundary: Compose -> ViewModel -> use case when rules exist -> repository -> DAO -> Room. Presentation never imports DAOs/entities; domain never imports Android APIs/resources.
- Use `collectAsStateWithLifecycle()`. Do not launch unmanaged application coroutines or block Main.
- Every route has a stateless content composable and deterministic `@Preview`; previews never resolve a Hilt ViewModel or open Room.
- UI strings go through resources. Domain results/errors contain no resource IDs.
- Preserve coroutine cancellation, expose recoverable errors, and disable duplicate write actions while in progress.
- Use TDD for every task: write focused failing tests, run and observe the intended failure, implement, rerun focused tests plus relevant regressions, then commit.
- Tasks 1-8 each produce exactly one implementation commit with the exact subject shown below. Do not combine, squash, amend across task boundaries, or create a final catch-all implementation commit.
- Do not silently rewrite M2 contracts. Any necessary extension must be focused, tested at repository/Room boundaries, and remain backward-compatible with existing tests.

---

### Task 1: Android-free workout validation

**Commit:** `feat(domain): add workout validation rules`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/validation/WorkoutValidation.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/domain/validation/WorkoutValidationTest.kt`

**Interfaces:**
- Produces `WorkoutValidation.validateName(String): NameResult` with a trimmed valid value.
- Produces `WorkoutValidation.validateExerciseIds(List<String>): ExerciseListResult`.
- Produces `WorkoutValidation.validateSet(reps: Int, weightKg: Double): SetResult`.
- Later editor/session tasks consume these results; no Android types are allowed.

- [ ] **Step 1: Write parameterized/table-driven failing unit tests**

Cover blank and whitespace-only names; lengths 1, 50, and 51 after trimming; returned trimmed name; zero exercises; one exercise; duplicate IDs; reps `-1`, `0`, `1`, `100`, `101`; weight `-0.1`, `0.0`, `1000.0`, `1000.1`, `NaN`, and infinities. Require distinct typed failures for name, exercise list, reps, and weight.

- [ ] **Step 2: Run the focused tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*WorkoutValidationTest" --no-daemon
```

Expected: FAIL because `WorkoutValidation` does not exist.

- [ ] **Step 3: Implement the minimal typed validator**

Use sealed results with these stable meanings:

```kotlin
object WorkoutValidation {
    const val MAX_NAME_LENGTH = 50
    const val MIN_REPS = 1
    const val MAX_REPS = 100
    const val MIN_WEIGHT_KG = 0.0
    const val MAX_WEIGHT_KG = 1000.0

    fun validateName(rawName: String): NameResult
    fun validateExerciseIds(exerciseIds: List<String>): ExerciseListResult
    fun validateSet(reps: Int, weightKg: Double): SetResult
}

sealed interface NameResult {
    data class Valid(val trimmedName: String) : NameResult
    data object Blank : NameResult
    data object TooLong : NameResult
}

sealed interface ExerciseListResult {
    data object Valid : ExerciseListResult
    data object Empty : ExerciseListResult
    data class Duplicate(val exerciseId: String) : ExerciseListResult
}

sealed interface SetResult {
    data object Valid : SetResult
    data object InvalidReps : SetResult
    data object InvalidWeight : SetResult
}
```

Reject non-finite weights. Duplicate detection preserves the first duplicated ID for deterministic UI feedback.

- [ ] **Step 4: Run focused tests and the existing JVM suite**

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

Expected: all existing and new JVM tests pass.

- [ ] **Step 5: Commit only Task 1**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/domain/validation app/src/test/java/com/haitranduc/fittrack/domain/validation
git commit -m "feat(domain): add workout validation rules"
```

### Task 2: Persisted exercise library and startup seed gate

**Commit:** `feat(exercises): connect exercise library to Room`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/startup/FitTrackViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/startup/StartupUiState.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseListViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseListUiState.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseDetailViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseDetailUiState.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/startup/FitTrackViewModelTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/exercise/ExerciseListViewModelTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/exercise/ExerciseDetailViewModelTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/testing/FakeExerciseRepository.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackNavHost.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseListScreen.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseDetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Delete when unused: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseMockData.kt`

**Interfaces:**
- Consumes the existing `ExerciseRepository` exactly as exposed by M2.
- Produces root startup `Loading`, `Ready`, and `Error` states with retry.
- Produces exercise list state containing query/filter selections and `List<Exercise>`.
- Produces exercise detail state loaded from a defensive `String` route ID.

- [ ] **Step 1: Write failing startup ViewModel tests**

Using a fake repository and `StandardTestDispatcher`, verify `Imported` and `AlreadySeeded` reach `Ready`, typed failure reaches `Error`, retry invokes `ensureSeeded()` again, and only one initial call occurs per ViewModel lifetime.

- [ ] **Step 2: Write failing exercise ViewModel tests**

Verify initial repository observation, query trimming, body filter, equipment filter, combined parameters, clearing filters to `null`, empty success, data failure, valid detail, missing detail, and detail failure. Assert stale collectors are cancelled when filters change.

- [ ] **Step 3: Run focused tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*FitTrackViewModelTest" --tests "*Exercise*ViewModelTest" --no-daemon
```

Expected: FAIL because the ViewModels/states do not exist.

- [ ] **Step 4: Implement startup and exercise ViewModels**

Annotate production ViewModels with `@HiltViewModel`. Use `viewModelScope`, `StateFlow`, `stateIn`, and `flatMapLatest`/`combine` for query and filters. Convert `DataResult` and `SeedImportResult` exhaustively; never silently turn a failure into an empty list.

- [ ] **Step 5: Split each screen into route and content composables**

The route resolves its ViewModel and collects lifecycle-aware state. The content accepts state plus callbacks. Render loading, content, empty, missing, and error/retry states. Keep existing visual language and content descriptions. Replace mock resource-backed exercise text with domain strings and retain previews using explicit sample `Exercise` values.

- [ ] **Step 6: Gate the navigation graph on startup readiness**

Resolve the root ViewModel once in `FitTrackNavHost`. Show a centered progress state while loading and a recoverable error/retry surface on failure. Build the existing `NavHost` only in `Ready`. Do not call `ensureSeeded()` from individual screens or `Application`.

- [ ] **Step 7: Run focused, navigation, and data regressions**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*FitTrackViewModelTest" --tests "*Exercise*ViewModelTest" --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.seed.ExerciseSeedImporterTest --no-daemon
```

Update M1 exercise UI assertions to use stable seed records/test tags rather than mock-only string resources. Expected: focused tests pass and the real 1,324-record seed is shown without a crash.

- [ ] **Step 8: Commit only Task 2**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/presentation/startup app/src/main/java/com/haitranduc/fittrack/presentation/exercise app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackNavHost.kt app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation app/src/test/java/com/haitranduc/fittrack/testing app/src/androidTest/java/com/haitranduc/fittrack/FitTrackNavigationTest.kt
git commit -m "feat(exercises): connect exercise library to Room"
```

### Task 3: Workout editor with persisted selection and ordering

**Commit:** `feat(workouts): implement workout editor`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/usecase/SaveWorkoutUseCase.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutEditorViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutEditorUiState.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/ExercisePickerContent.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/domain/usecase/SaveWorkoutUseCaseTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/workout/WorkoutEditorViewModelTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/testing/FakeWorkoutRepository.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutEditorScreen.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackNavHost.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackDestination.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes `WorkoutValidation`, `ExerciseRepository`, and `WorkoutRepository`.
- Produces `suspend operator fun SaveWorkoutUseCase(workout: Workout): SaveWorkoutResult`.
- Produces a saved `Long` workout ID event for navigation/start; route IDs are parsed defensively.

- [ ] **Step 1: Write failing save-use-case tests**

Verify blank/overlong names and empty/duplicate exercise lists do not call the repository; valid names are trimmed; create uses `id = 0`, preserves the supplied `createdAt`, updates `updatedAt`, preserves exercise order, returns the inserted ID, and maps repository failure to a typed result.

- [ ] **Step 2: Write failing editor ViewModel tests**

Cover create initial state, edit loading/missing/error, name changes, open/close picker, searchable persisted exercise choices, add, duplicate prevention, remove, move up/down boundary behavior, save validation, save success event, save failure, and prevention of double save.

- [ ] **Step 3: Run focused tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*SaveWorkoutUseCaseTest" --tests "*WorkoutEditorViewModelTest" --no-daemon
```

Expected: FAIL because use case/editor ViewModel types do not exist.

- [ ] **Step 4: Implement the save use case and editor ViewModel**

Use a focused time source local to the use case or the shared `TimeProvider` interface introduced now if Task 5 will consume it. New workouts set both timestamps to now; edits retain `createdAt` and update only `updatedAt`. Model one-shot navigation using a consumed event or channel, not a boolean that replays after rotation.

- [ ] **Step 5: Implement the stateless editor and exercise picker**

Replace all `remember` business state and mock additions. Add/remove/reorder persisted exercises; use explicit up/down actions. Show field/list validation, progress, missing/error, retry, and save states. The Save action navigates only after repository success. Keep a preview for create and populated edit content.

- [ ] **Step 6: Update navigation to persisted workout IDs**

Use `Long` in callbacks and `workout_editor/{workoutId}` routes. Invalid route values must render missing/error state. Do not start a session in this task; expose a stable callback/event with the saved workout ID for Task 6.

- [ ] **Step 7: Run focused tests and existing repository tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*SaveWorkoutUseCaseTest" --tests "*WorkoutEditorViewModelTest" --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.repository.RepositoryIntegrationTest --no-daemon
```

Expected: editor tests and existing ordered-workout repository tests pass.

- [ ] **Step 8: Commit only Task 3**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/domain/usecase/SaveWorkoutUseCase.kt app/src/main/java/com/haitranduc/fittrack/presentation/workout app/src/main/java/com/haitranduc/fittrack/core/navigation app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/domain/usecase/SaveWorkoutUseCaseTest.kt app/src/test/java/com/haitranduc/fittrack/presentation/workout app/src/test/java/com/haitranduc/fittrack/testing
git commit -m "feat(workouts): implement workout editor"
```

### Task 4: Workout list, edit entry, and safe deletion

**Commit:** `feat(workouts): implement workout list and deletion`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutListViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutListUiState.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/workout/WorkoutListViewModelTest.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutListScreen.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackNavHost.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Delete when unused: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutMockData.kt`

**Interfaces:**
- Consumes `WorkoutRepository.observeWorkouts()` and `delete(Long)`.
- Produces list loading/content/empty/error states, edit/create callbacks, and a confirmed delete event.

- [ ] **Step 1: Write failing list ViewModel tests**

Verify loading, ordered content, empty success, observation failure/retry, delete confirmation open/dismiss, successful delete, delete failure, no delete before confirmation, and double-delete prevention.

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*WorkoutListViewModelTest" --no-daemon
```

Expected: FAIL because the list ViewModel/state does not exist.

- [ ] **Step 3: Implement the ViewModel and stateless list content**

Observe persisted workouts, show their real names/exercise counts/names, and expose create/edit/delete events using `Long` IDs. Add a destructive-action confirmation dialog and visible retry/error state. Preserve the M1 FAB and visual hierarchy. Retain empty/populated previews.

- [ ] **Step 4: Verify delete preserves completed history**

Run the existing Room test that deletes a workout and retains session/set snapshots. If production behavior does not match, stop and fix the owning repository/DAO contract with a focused regression test in this task; do not change schema version or use destructive migration.

- [ ] **Step 5: Run focused and history-retention regressions**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*WorkoutListViewModelTest" --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.WorkoutHistoryDaoTest --no-daemon
```

Expected: list tests pass and template deletion retains completed history.

- [ ] **Step 6: Commit only Task 4**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/presentation/workout app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackNavHost.kt app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation/workout app/src/androidTest
git commit -m "feat(workouts): implement workout list and deletion"
```

### Task 5: Start, complete-set, and finish use cases

**Commit:** `feat(sessions): implement workout session use cases`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/time/TimeProvider.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/usecase/StartWorkoutUseCase.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/usecase/CompleteSetUseCase.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/usecase/FinishWorkoutUseCase.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/usecase/SessionResults.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/core/time/SystemTimeProvider.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/core/time/TimeModule.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/domain/usecase/StartWorkoutUseCaseTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/domain/usecase/CompleteSetUseCaseTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/domain/usecase/FinishWorkoutUseCaseTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/testing/FakeWorkoutHistoryRepository.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/domain/repository/WorkoutHistoryRepository.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/data/repository/WorkoutHistoryRepositoryImpl.kt`
- Modify if required: `app/src/main/java/com/haitranduc/fittrack/data/local/dao/WorkoutSessionDao.kt`
- Modify: `app/src/androidTest/java/com/haitranduc/fittrack/data/repository/RepositoryIntegrationTest.kt`

**Interfaces:**
- Produces `TimeProvider.nowEpochMillis(): Long`.
- Produces `StartWorkoutResult.Success(sessionId)`, `WorkoutNotFound`, `EmptyWorkout`, `ActiveSessionExists(sessionId)`, and `Failure`.
- Produces typed complete-set and finish results described by the design spec.
- Extends the history repository only with focused active/one-shot reads needed to enforce rules.

- [ ] **Step 1: Write failing StartWorkoutUseCase tests**

Cover valid start and exact snapshot/timestamp persistence, missing workout, zero exercises, existing active session returning its ID, repository read/write failures, cancellation, and two concurrent starts resulting in exactly one insert. Use a `Mutex` inside the singleton use case (or an equivalent atomic repository transaction) to serialize in-process checks.

- [ ] **Step 2: Write failing CompleteSetUseCase tests**

Cover reps `1/100` and weight `0/1000` success, every invalid boundary including non-finite weight, missing session, finished session, sequential set numbering, timestamp/snapshot persistence, duplicate/database failure, no write on validation error, and cancellation.

- [ ] **Step 3: Write failing FinishWorkoutUseCase tests**

Cover success with at least one completed set, no-set rejection, missing and already-finished session, exact `finishedAt`, floor-to-seconds nonnegative duration, update failure, prevention of a second finish, and cancellation.

- [ ] **Step 4: Run focused tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*StartWorkoutUseCaseTest" --tests "*CompleteSetUseCaseTest" --tests "*FinishWorkoutUseCaseTest" --no-daemon
```

Expected: FAIL because session use cases/results/time source do not exist.

- [ ] **Step 5: Extend the repository boundary and implement use cases**

Add exact one-shot operations rather than collecting an unbounded Flow inside every use case. A suitable contract is:

```kotlin
suspend fun getActiveSession(): DataResult<WorkoutSession?>
suspend fun getSession(id: Long): DataResult<WorkoutSession?>
```

If `getSession` needs sets, implement it transactionally or by a deterministic DAO projection. All new implementation paths map storage errors consistently with M2 and rethrow cancellation. Implement use cases without Android imports and inject `TimeProvider`.

- [ ] **Step 6: Wire the system time provider with Hilt**

`SystemTimeProvider` returns `System.currentTimeMillis()`. Bind it as a singleton domain `TimeProvider`; tests supply a mutable fake. Do not call wall-clock APIs directly in use cases.

- [ ] **Step 7: Run unit and repository/DAO regressions**

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.repository.RepositoryIntegrationTest --no-daemon
```

Expected: all use-case/JVM tests and M2 repository integration tests pass.

- [ ] **Step 8: Commit only Task 5**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/domain/time app/src/main/java/com/haitranduc/fittrack/domain/usecase app/src/main/java/com/haitranduc/fittrack/core/time app/src/main/java/com/haitranduc/fittrack/domain/repository/WorkoutHistoryRepository.kt app/src/main/java/com/haitranduc/fittrack/data app/src/test/java/com/haitranduc/fittrack/domain/usecase app/src/test/java/com/haitranduc/fittrack/testing app/src/androidTest/java/com/haitranduc/fittrack/data/repository/RepositoryIntegrationTest.kt
git commit -m "feat(sessions): implement workout session use cases"
```

### Task 6: Persisted active-workout flow

**Commit:** `feat(active-workout): implement active workout flow`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/activeworkout/ActiveWorkoutViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/activeworkout/ActiveWorkoutUiState.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/activeworkout/ActiveWorkoutViewModelTest.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/activeworkout/ActiveWorkoutScreen.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutEditorViewModel.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/workout/WorkoutEditorScreen.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackDestination.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackNavHost.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes the three Task 5 use cases and persisted repositories.
- Produces `active_workout/{sessionId}` and navigation with the returned session ID.
- Produces per-exercise reps/weight input, persisted completed sets, elapsed time, and finish events.

- [ ] **Step 1: Write failing active ViewModel tests**

Verify defensive session-ID parsing; initial loading/content/missing/error; template exercise loading; reps/weight text changes; invalid parse/range feedback; next set number; successful immediate completion; persistence failure; finish rejection with no sets; finish success event; duplicate-tap prevention; elapsed time from persisted `startedAt`; and restoration from `SavedStateHandle` without inserting a second session.

- [ ] **Step 2: Extend editor tests for Start**

Verify Start validates and saves pending changes first, invokes `StartWorkoutUseCase` with the real saved ID, handles active-session conflict deterministically, exposes errors, and emits navigation exactly once.

- [ ] **Step 3: Run focused tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ActiveWorkoutViewModelTest" --tests "*WorkoutEditorViewModelTest" --no-daemon
```

Expected: FAIL because active ViewModel/integration does not exist.

- [ ] **Step 4: Implement active ViewModel and editor start orchestration**

Observe session/sets as source of truth. Load template exercises using nullable `workoutId`; missing templates produce a clear state rather than a crash. Keep editable input keyed by exercise ID in ViewModel/SavedStateHandle. Completed-set success must appear only after `insertSet` succeeds. Timer ticks may be in memory, but derives from persisted start time.

- [ ] **Step 5: Implement stateless active content**

Replace mock set rows with real exercises and completed sets. Provide numeric keyboard hints, accessible complete-set buttons, inline validation, finish progress/error, and missing/error retry/back behavior. Keep deterministic populated/empty previews without a ViewModel.

- [ ] **Step 6: Update navigation/back-stack behavior**

Generate and parse `active_workout/{sessionId}`. On successful finish, remove active workout from the back stack and navigate to `history_detail/{sessionId}` or the History graph with the finished ID. Back must not reopen a finished active screen.

- [ ] **Step 7: Run focused tests and navigation instrumentation tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ActiveWorkoutViewModelTest" --tests "*WorkoutEditorViewModelTest" --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.FitTrackNavigationTest --no-daemon
```

Expected: ViewModel tests pass and navigation tests use persisted IDs/data without a crash.

- [ ] **Step 8: Commit only Task 6**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/presentation/activeworkout app/src/main/java/com/haitranduc/fittrack/presentation/workout app/src/main/java/com/haitranduc/fittrack/core/navigation app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation app/src/androidTest/java/com/haitranduc/fittrack/FitTrackNavigationTest.kt
git commit -m "feat(active-workout): implement active workout flow"
```

### Task 7: Persisted history list and detail

**Commit:** `feat(history): connect workout history to Room`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryUiState.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryDetailViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryDetailUiState.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/history/HistoryViewModelTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/history/HistoryDetailViewModelTest.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryScreen.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryDetailScreen.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackDestination.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackNavHost.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Delete when unused: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryMockData.kt`

**Interfaces:**
- Consumes `WorkoutHistoryRepository.observeHistory()` and `observeSession(Long)`.
- Produces finished-only history list and snapshot-based detail UI using persisted `Long` IDs.

- [ ] **Step 1: Write failing history ViewModel tests**

Cover list loading, empty, ordered success, failure/retry; detail defensive ID parsing, loading, success with ordered sets, missing, and failure/retry. Assert displayed models use workout/exercise snapshot names, stored reps/weight, timestamps, and duration rather than current templates.

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HistoryViewModelTest" --tests "*HistoryDetailViewModelTest" --no-daemon
```

Expected: FAIL because history ViewModels/states do not exist.

- [ ] **Step 3: Implement ViewModels and stateless history content**

Collect repository flows lifecycle-safely through route composables. Format timestamps/duration/weight in presentation code, not domain. Render loading, empty/missing, content, and error/retry. Preserve list/detail previews with explicit snapshot samples.

- [ ] **Step 4: Remove all remaining production mock-data callers**

Run:

```powershell
rg "ExerciseMockData|WorkoutMockData|HistoryMockData" app/src/main
```

Expected after cleanup: no matches. Delete mock files only after previews and screens no longer use them.

- [ ] **Step 5: Verify snapshot retention and restart-backed behavior**

Run history DAO/repository instrumentation tests, including deletion of the original workout. Add an instrumentation assertion that closes/reopens a file-backed test database and still reads the finished session/detail.

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HistoryViewModelTest" --tests "*HistoryDetailViewModelTest" --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.WorkoutHistoryDaoTest --no-daemon
```

Expected: finished history survives template deletion and file-backed database reopen.

- [ ] **Step 6: Commit only Task 7**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/presentation/history app/src/main/java/com/haitranduc/fittrack/core/navigation app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation/history app/src/androidTest
git commit -m "feat(history): connect workout history to Room"
```

### Task 8: M3 critical-flow instrumentation and completion gate

**Commit:** `test(m3): verify critical workout flow`

**Files:**
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/M3CriticalFlowTest.kt`
- Modify: `app/src/androidTest/java/com/haitranduc/fittrack/FitTrackNavigationTest.kt`
- Modify only if test isolation requires it: `app/build.gradle.kts`
- Modify only if test isolation requires it: `gradle/libs.versions.toml`
- Create after the commit as an untracked project report: `../FitTrack_Project_Plan/phase-results/PHASE_3_RESULT.md`

**Interfaces:**
- Verifies the real production navigation/ViewModel/repository/Room path, not mock screen state.
- Produces stable semantics/test tags only where visible text is not a reliable selector.

- [ ] **Step 1: Write the failing isolated critical-flow test**

Use a unique workout name and deterministic database isolation. Drive this complete flow through Compose UI:

```text
launch -> seed ready -> Workouts -> Create -> enter valid name
-> Add Exercise -> choose a real seeded exercise -> Save
-> reopen/edit saved workout -> Start
-> enter reps 10 and weight 50 -> Complete Set
-> Finish -> History Detail/List
```

Assert the saved workout, completed set, workout snapshot name, exercise snapshot name, reps, weight, and nonnegative duration are visible. Recreate the Activity and confirm history remains. Delete the template and confirm completed history remains readable. Do not depend on test execution order or a developer's existing production database.

- [ ] **Step 2: Run only the new test and confirm the intended failure**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.M3CriticalFlowTest --no-daemon
```

Expected before completing test hooks/assertions: FAIL for a specific missing selector/isolation behavior, not because no emulator is available. If there is no device, start an AVD and rerun; do not call that a product failure.

- [ ] **Step 3: Complete deterministic test isolation and stable selectors**

Prefer existing AndroidX/Room/Compose test tools. If Hilt test replacement is genuinely required, add only official Hilt testing/compiler dependencies matching Hilt `2.59.2`; do not add a mocking framework. Test tags describe semantics and do not replace accessibility content descriptions.

- [ ] **Step 4: Run all M3 and regression gates**

From `FitTrack/`:

```powershell
python -m unittest tools/test_prepare_exercise_seed.py -v
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --no-daemon
.\gradlew.bat connectedDebugAndroidTest --no-daemon
git diff --check
```

Expected: every command exits `0`; the Python seed suite reports 6 passing tests; no M0-M2 test is disabled or deleted to obtain green status.

- [ ] **Step 5: Perform runtime and scope audits**

Install/launch the debug APK on an emulator and manually smoke the critical flow. Confirm no production mock references, no dataset media packaged, no destructive migration, no network dependency, all route/content screens have previews, and `git diff --check` is clean.

- [ ] **Step 6: Commit only Task 8**

```powershell
git add app/src/androidTest app/build.gradle.kts gradle/libs.versions.toml
git commit -m "test(m3): verify critical workout flow"
```

Do not create another implementation commit. If a completion-gate failure reveals an implementation defect, stop and report the exact evidence; fix it in the owning task while preserving the agreed one-task-one-commit history rather than hiding it in a catch-all commit.

- [ ] **Step 7: Write the external phase report without staging it**

Create `../FitTrack_Project_Plan/phase-results/PHASE_3_RESULT.md` containing branch/commit list, test counts and commands, emulator/API used, critical-flow evidence, preview/mock audit, known limitations, and final PASS/FAIL. This file is outside the code folder and must not be added to the FitTrack Git history.

## Self-review

- Spec coverage: Task 1 owns pure validation; Task 2 owns startup and the persisted exercise library; Tasks 3-4 own complete template CRUD and ordering; Task 5 owns all session rules; Task 6 owns active UI/state; Task 7 owns persisted history; Task 8 owns the real critical flow and full completion gate.
- Placeholder scan: every required behavior has an owning task, command, expected result, and commit; optional M4 work is explicitly excluded.
- Type consistency: workout/session navigation IDs are `Long`; repository persistence continues using `Workout`, `WorkoutSession`, `SetLog`, `DataResult`, and typed extensions; time comes only through `TimeProvider` in domain use cases.
