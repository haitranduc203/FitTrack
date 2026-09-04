# M4 Secondary Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lifecycle-safe rest timer, persistent exercise favorites, persistent theme selection, and three reactive lifetime statistics without regressing the M3 offline workout flow.

**Architecture:** Keep Room as the source of truth for favorites and finished-session aggregates, and use Preferences DataStore only for theme selection. Hilt ViewModels combine repository flows into immutable UI state; periodic scheduling remains in the lifecycle-bound Compose effect while the ViewModel performs synchronous timestamp calculations.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Navigation Compose, Hilt, Room 2.7.0, Preferences DataStore 1.1.3, Coroutines/Flow, JUnit4, AndroidX Room testing, Compose UI testing.

**Spec:** `docs/superpowers/specs/2026-09-04-m4-secondary-features-design.md`

## Global Constraints

- Work only on `feature/secondary-features`, based on merged M3 commit `16eb4d0` plus planning commits.
- Keep the app offline; do not add networking, authentication, analytics, notifications, sound, vibration, or background services.
- Use TDD per task: focused failing test, intended failure, minimal implementation, regression, then one commit.
- Produce exactly six ordered implementation commits; never squash them or add a catch-all commit.
- Use string resources for user-visible/accessibility text. Domain/data layers contain no Android resource IDs.
- Keep Room data on migration; never call `fallbackToDestructiveMigration()`.
- Preserve all M0-M3 tests and deterministic content `@Preview` functions.
- Never put `while`, periodic `delay`, `ticker`, or self-rescheduling work in a ViewModel, use case, repository, fake, or unit test.
- The only periodic coroutine allowed is the cancellable `LaunchedEffect` in `ActiveWorkoutScreen`; tests call `onTimerTick()` manually.
- Use 120-second process timeouts for focused tests and 360 seconds for full/device suites. Diagnose every timeout; never wait indefinitely or disable a test.
- Run commands from `FitTrack/`. Write `../FitTrack_Project_Plan/phase-results/PHASE_4_RESULT.md` without staging it.

---

### Task 1: Lifecycle-safe 90-second rest timer

**Commit:** `feat(active-workout): add rest timer`

**Files:**
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/activeworkout/ActiveWorkoutUiState.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/activeworkout/ActiveWorkoutViewModel.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/activeworkout/ActiveWorkoutScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/test/java/com/haitranduc/fittrack/presentation/activeworkout/ActiveWorkoutViewModelTest.kt`

**Interfaces:** Add `REST_DURATION_SECONDS = 90L`, saved-state key `rest_timer_ends_at_millis`, state fields `restTimerEndsAtMillis: Long?` and `restTimerRemainingSeconds: Long`, plus synchronous `onSkipRestTimer()`. Start/restart only after `CompleteSetResult.Success`.

- [ ] **Step 1: Write focused failing ViewModel tests**

Use `FakeTimeProvider` and `SavedStateHandle` to assert:

```kotlin
assertEquals(90L, viewModel.uiState.value.restTimerRemainingSeconds)
fakeTimeProvider.advanceBy(30_000L)
viewModel.onTimerTick()
assertEquals(60L, viewModel.uiState.value.restTimerRemainingSeconds)
viewModel.onSkipRestTimer()
assertEquals(0L, viewModel.uiState.value.restTimerRemainingSeconds)
assertNull(savedStateHandle.get<Long>(rest_timer_ends_at_millis))
```

Cover success, persistence failure not starting a timer, another success resetting to 90, exact-zero expiry, restored expired deadline, and ViewModel recreation. Never launch a periodic coroutine in tests.

- [ ] **Step 2: Run focused tests and confirm red**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests *ActiveWorkoutViewModelTest --rerun-tasks --no-daemon
```

Expected: missing timer state/actions; process terminates within 120 seconds.

- [ ] **Step 3: Implement timestamp-based state**

```kotlin
private fun startRestTimer(now: Long = timeProvider.currentTimeMillis()) {
    val endsAt = now + REST_DURATION_SECONDS * 1_000L
    savedStateHandle[REST_TIMER_ENDS_AT_KEY] = endsAt
    updateRestTimer(now, endsAt)
}

private fun updateRestTimer(now: Long, endsAt: Long?) {
    val remaining = endsAt?.let { maxOf(0L, (it - now + 999L) / 1_000L) } ?: 0L
    if (remaining == 0L) savedStateHandle[REST_TIMER_ENDS_AT_KEY] = null
    _uiState.update { it.copy(restTimerEndsAtMillis = endsAt?.takeIf { remaining > 0L }, restTimerRemainingSeconds = remaining) }
}
```

Capture one `now` in `onTimerTick()` and update elapsed/rest synchronously. `onSkipRestTimer()` clears saved/UI state synchronously.

- [ ] **Step 4: Add stateless rest-timer UI**

Render only while remaining is positive. Show localized Rest, `mm:ss`, and Skip; add `onSkipRestTimer` from route to content. Reuse the existing `LaunchedEffect`; do not add another loop. Update running/absent previews.

- [ ] **Step 5: Verify and audit**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests *ActiveWorkoutViewModelTest --rerun-tasks --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

Run `rg` for `while`, `delay`, and `ticker` in main/test sources. Expected: tests pass and periodic work exists only in `ActiveWorkoutScreen.kt`.

- [ ] **Step 6: Commit only Task 1**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/presentation/activeworkout app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation/activeworkout
git commit -m 'feat(active-workout): add rest timer'
```

### Task 2: Persistent exercise favorites with Room migration 1 to 2

**Commit:** `feat(exercises): add persistent favorites`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/entity/FavoriteExerciseEntity.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/dao/FavoriteExerciseDao.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackMigrations.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/repository/FavoriteExerciseRepository.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/repository/FavoriteExerciseRepositoryImpl.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/testing/FakeFavoriteExerciseRepository.kt`
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/core/database/Migration1To2Test.kt`
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/FavoriteExerciseDaoTest.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackDatabase.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/database/DatabaseModule.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/data/repository/RepositoryModule.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseListUiState.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseListViewModel.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseListScreen.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseDetailUiState.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseDetailViewModel.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/exercise/ExerciseDetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/test/java/com/haitranduc/fittrack/presentation/exercise/ExerciseListViewModelTest.kt`
- Modify: `app/src/test/java/com/haitranduc/fittrack/presentation/exercise/ExerciseDetailViewModelTest.kt`
- Modify: `app/src/androidTest/java/com/haitranduc/fittrack/core/database/DatabaseContractTest.kt`
- Modify: `app/src/androidTest/java/com/haitranduc/fittrack/data/repository/RepositoryIntegrationTest.kt`
- Generate: `app/schemas/com.haitranduc.fittrack.core.database.FitTrackDatabase/2.json`

**Interfaces:**

```kotlin
interface FavoriteExerciseRepository {
    fun observeFavoriteIds(): Flow<DataResult<Set<String>>>
    suspend fun setFavorite(exerciseId: String, isFavorite: Boolean): DataResult<Unit>
}

data class FavoriteExerciseEntity(
    @PrimaryKey val exerciseId: String,
    val createdAt: Long
)
```

The entity table is `favorite_exercises` with a foreign key to `exercises(id)` and `ON DELETE CASCADE`.

- [ ] **Step 1: Write failing migration and DAO tests**

Create representative v1 exercise/workout/session/set rows, run `MIGRATION_1_2`, prove all old rows remain, then insert/read/delete a favorite. Update the contract test for version 2, the new table/foreign key, and `favoriteExerciseDao()`. DAO tests cover ordered IDs plus idempotent insert/delete. Repository integration verifies observation, writes, reopen persistence, typed failures, and cancellation; inject `TimeProvider` for deterministic `createdAt`.

- [ ] **Step 2: Write failing list/detail tests**

Use `FakeFavoriteExerciseRepository` to cover Favorites combined with query/body/equipment filters, list/detail synchronization, toggle error/retry, and duplicate taps while a write is pending.

- [ ] **Step 3: Run focused tests and confirm red**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ExerciseListViewModelTest" --tests "*ExerciseDetailViewModelTest" --rerun-tasks --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.core.database.Migration1To2Test --no-daemon
```

Expected: failure for missing favorites/migration within finite timeouts.

- [ ] **Step 4: Implement schema version 2**

`MIGRATION_1_2` executes schema-equivalent SQL:

```sql
CREATE TABLE IF NOT EXISTS `favorite_exercises` (
  `exerciseId` TEXT NOT NULL,
  `createdAt` INTEGER NOT NULL,
  PRIMARY KEY(`exerciseId`),
  FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
)
```

Add entity/DAO to `FitTrackDatabase`, set version 2, and register `.addMigrations(MIGRATION_1_2)`. Keep canonical `ExerciseEntity` unchanged and never use destructive migration.

- [ ] **Step 5: Implement repository and exercise UI**

Map DAO failures to `DataError.Database` and preserve cancellation. Add favorite IDs, Favorites-only filter, write-in-progress IDs, and localized failure to list/detail state. Star buttons expose selected semantics and localized favorite/unfavorite descriptions. Keep query/body/equipment filtering database-backed, then apply favorite membership.

- [ ] **Step 6: Verify migration and regressions**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ExerciseListViewModelTest" --tests "*ExerciseDetailViewModelTest" --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.core.database.Migration1To2Test,com.haitranduc.fittrack.data.local.dao.FavoriteExerciseDaoTest,com.haitranduc.fittrack.core.database.DatabaseContractTest --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

Expected: old data survives migration, favorites persist, and JVM regressions pass.

- [ ] **Step 7: Commit only Task 2**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/core/database app/src/main/java/com/haitranduc/fittrack/data/local app/src/main/java/com/haitranduc/fittrack/data/repository app/src/main/java/com/haitranduc/fittrack/domain/repository app/src/main/java/com/haitranduc/fittrack/presentation/exercise app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation/exercise app/src/test/java/com/haitranduc/fittrack/testing app/src/androidTest app/schemas
git commit -m "feat(exercises): add persistent favorites"
```

### Task 3: Persistent System/Light/Dark theme setting

**Commit:** `feat(settings): add theme preference`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/model/ThemePreference.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/repository/ThemePreferenceRepository.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/preferences/ThemePreferenceRepositoryImpl.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/core/preferences/PreferencesModule.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/theme/ThemeViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/settings/SettingsUiState.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/presentation/settings/SettingsScreen.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/data/preferences/ThemePreferenceRepositoryImplTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/theme/ThemeViewModelTest.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/presentation/settings/SettingsViewModelTest.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/MainActivity.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackDestination.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/navigation/FitTrackNavHost.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/designsystem/FitTrackIcons.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/androidTest/java/com/haitranduc/fittrack/FitTrackNavigationTest.kt`

**Interfaces:**

```kotlin
enum class ThemePreference { SYSTEM, LIGHT, DARK }

interface ThemePreferenceRepository {
    fun observeThemePreference(): Flow<DataResult<ThemePreference>>
    suspend fun setThemePreference(preference: ThemePreference): DataResult<Unit>
}
```

Inject `DataStore<Preferences>` from `PreferenceDataStoreFactory`; persist key `theme_preference` with enum names. Unknown values map to `SYSTEM`.

- [ ] **Step 1: Write failing repository/ViewModel tests**

Use a temporary DataStore file and cancel its scope after every test. Assert default System, Light/Dark round trip, invalid raw value fallback, read/write failure, cancellation, loading, retry, write-in-progress, successful selection, and failed write retaining the prior selection.

- [ ] **Step 2: Write failing navigation/persistence test**

Extend `FitTrackNavigationTest` to open Settings, assert System/Light/Dark, choose Dark, recreate Activity, confirm Dark remains selected, return to Exercises, and verify the original tabs still restore state.

- [ ] **Step 3: Run focused tests and confirm red**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ThemePreferenceRepositoryImplTest" --tests "*ThemeViewModelTest" --tests "*SettingsViewModelTest" --rerun-tasks --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.FitTrackNavigationTest --no-daemon
```

Expected: failure for missing repository/ViewModels/Settings destination.

- [ ] **Step 4: Implement DataStore and root theme**

Bind singleton `DataStore<Preferences>` named `fittrack_settings.preferences_pb` and the repository. `ThemeViewModel` exposes the value without blocking the main thread. In `MainActivity`:

```kotlin
val useDarkTheme = when (themePreference) {
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}
FitTrackTheme(darkTheme = useDarkTheme) { FitTrackNavHost() }
```

- [ ] **Step 5: Implement Settings UI/navigation**

Add `SETTINGS = "settings"` as fourth top-level destination with `launchSingleTop`, `saveState`, and `restoreState`. Render three Material 3 single-choice rows, disable concurrent writes, show localized error/retry, and retain loading/content/error previews.

- [ ] **Step 6: Verify persistence and regressions**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ThemePreferenceRepositoryImplTest" --tests "*ThemeViewModelTest" --tests "*SettingsViewModelTest" --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.FitTrackNavigationTest --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

Expected: selection persists through recreation/reopen, four destinations work, and JVM regressions pass.

- [ ] **Step 7: Commit only Task 3**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/MainActivity.kt app/src/main/java/com/haitranduc/fittrack/core/preferences app/src/main/java/com/haitranduc/fittrack/core/navigation app/src/main/java/com/haitranduc/fittrack/core/designsystem app/src/main/java/com/haitranduc/fittrack/data/preferences app/src/main/java/com/haitranduc/fittrack/domain app/src/main/java/com/haitranduc/fittrack/presentation/settings app/src/main/java/com/haitranduc/fittrack/presentation/theme app/src/main/res/values/strings.xml app/src/test app/src/androidTest/java/com/haitranduc/fittrack/FitTrackNavigationTest.kt
git commit -m "feat(settings): add theme preference"
```

### Task 4: Reactive total completed-workouts statistic

**Commit:** `feat(statistics): add total workouts`

**Files:**
- Create: `app/src/main/java/com/haitranduc/fittrack/data/local/dao/StatisticsDao.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/domain/repository/StatisticsRepository.kt`
- Create: `app/src/main/java/com/haitranduc/fittrack/data/repository/StatisticsRepositoryImpl.kt`
- Create: `app/src/test/java/com/haitranduc/fittrack/testing/FakeStatisticsRepository.kt`
- Create: `app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/StatisticsDaoTest.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/database/FitTrackDatabase.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/core/database/DatabaseModule.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/data/repository/RepositoryModule.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryUiState.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryViewModel.kt`
- Modify: `app/src/main/java/com/haitranduc/fittrack/presentation/history/HistoryScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/test/java/com/haitranduc/fittrack/presentation/history/HistoryViewModelTest.kt`
- Modify: `app/src/androidTest/java/com/haitranduc/fittrack/core/database/DatabaseContractTest.kt`
- Modify: `app/src/androidTest/java/com/haitranduc/fittrack/data/repository/RepositoryIntegrationTest.kt`

**Interfaces:**

```kotlin
interface StatisticsRepository {
    fun observeTotalWorkouts(): Flow<DataResult<Long>>
}

@Query("SELECT COUNT(*) FROM workout_sessions WHERE finishedAt IS NOT NULL")
fun observeTotalWorkouts(): Flow<Long>
```

- [ ] **Step 1: Write failing DAO/repository/ViewModel tests**

Assert empty zero, unfinished exclusion, finished count, reactive update after Finish, real repository observation/failure mapping, retry, and rendering while the history list is empty.

- [ ] **Step 2: Confirm red**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HistoryViewModelTest" --rerun-tasks --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.StatisticsDaoTest --no-daemon
```

- [ ] **Step 3: Implement aggregate and first summary value**

Add `statisticsDao()` and Hilt bindings without changing Room version 2. Map flow failures and cancellation correctly. Add `totalWorkouts: Long = 0L` and focused statistics loading/error to History state. Render a three-slot summary above History; populate Workouts and use neutral placeholders for fields owned by Tasks 5-6.

- [ ] **Step 4: Verify and commit only Task 4**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HistoryViewModelTest" --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.StatisticsDaoTest,com.haitranduc.fittrack.core.database.DatabaseContractTest --no-daemon
git add app/src/main/java/com/haitranduc/fittrack/data/local/dao/StatisticsDao.kt app/src/main/java/com/haitranduc/fittrack/domain/repository/StatisticsRepository.kt app/src/main/java/com/haitranduc/fittrack/data/repository/StatisticsRepositoryImpl.kt app/src/main/java/com/haitranduc/fittrack/core/database app/src/main/java/com/haitranduc/fittrack/data/repository/RepositoryModule.kt app/src/main/java/com/haitranduc/fittrack/presentation/history app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation/history app/src/test/java/com/haitranduc/fittrack/testing/FakeStatisticsRepository.kt app/src/androidTest
git commit -m "feat(statistics): add total workouts"
```

### Task 5: Reactive total completed-sets statistic

**Commit:** `feat(statistics): add total completed sets`

**Files:** Modify `StatisticsDao.kt`, `StatisticsRepository.kt`, `StatisticsRepositoryImpl.kt`, `HistoryUiState.kt`, `HistoryViewModel.kt`, `HistoryScreen.kt`, `strings.xml`, `FakeStatisticsRepository.kt`, `HistoryViewModelTest.kt`, and `StatisticsDaoTest.kt` created in Task 4.

**Interfaces:**

```kotlin
fun observeTotalCompletedSets(): Flow<DataResult<Long>>

@Query("""
    SELECT COUNT(*) FROM set_logs
    INNER JOIN workout_sessions ON workout_sessions.id = set_logs.sessionId
    WHERE workout_sessions.finishedAt IS NOT NULL
""")
fun observeTotalCompletedSets(): Flow<Long>
```

- [ ] **Step 1: Write failing aggregate/History tests**

Assert empty zero, exclusion of sets in unfinished sessions, inclusion after Finish, multiple-set totals, reactive update, repository failure/retry, and UI rendering.

- [ ] **Step 2: Confirm red**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HistoryViewModelTest" --rerun-tasks --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.StatisticsDaoTest --no-daemon
```

- [ ] **Step 3: Implement query/repository/second summary value**

Add `totalCompletedSets: Long = 0L` and combine it with existing history/total-workout flows. SQL performs the aggregate; do not load all sessions into Kotlin. Failure remains localized/retryable without discarding already-loaded history.

- [ ] **Step 4: Verify and commit only Task 5**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HistoryViewModelTest" --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.StatisticsDaoTest --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
git add app/src/main/java/com/haitranduc/fittrack/data/local/dao/StatisticsDao.kt app/src/main/java/com/haitranduc/fittrack/domain/repository/StatisticsRepository.kt app/src/main/java/com/haitranduc/fittrack/data/repository/StatisticsRepositoryImpl.kt app/src/main/java/com/haitranduc/fittrack/presentation/history app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation/history app/src/test/java/com/haitranduc/fittrack/testing/FakeStatisticsRepository.kt app/src/androidTest/java/com/haitranduc/fittrack/data/local/dao/StatisticsDaoTest.kt
git commit -m "feat(statistics): add total completed sets"
```

### Task 6: Reactive total training-time statistic and M4 gate

**Commit:** `feat(statistics): add total training time`

**Files:** Modify `StatisticsDao.kt`, `StatisticsRepository.kt`, `StatisticsRepositoryImpl.kt`, `HistoryUiState.kt`, `HistoryViewModel.kt`, `HistoryScreen.kt`, `strings.xml`, `FakeStatisticsRepository.kt`, `HistoryViewModelTest.kt`, `StatisticsDaoTest.kt`, and `M3CriticalFlowTest.kt`. After the commit, create external `../FitTrack_Project_Plan/phase-results/PHASE_4_RESULT.md`.

**Interfaces:**

```kotlin
fun observeTotalTrainingTimeSeconds(): Flow<DataResult<Long>>

@Query("""
    SELECT CAST(TOTAL(
        CASE WHEN durationSeconds > 0 THEN durationSeconds ELSE 0 END
    ) AS INTEGER)
    FROM workout_sessions
    WHERE finishedAt IS NOT NULL
""")
fun observeTotalTrainingTimeSeconds(): Flow<Long>
```

Presentation formats raw seconds as `0m`, `59m`, `1h 0m`, or `2h 5m`.

- [ ] **Step 1: Write failing duration/format tests**

Assert empty zero, unfinished exclusion, null/negative duration clamping, sum across finished sessions, reactive update, repository failure/retry, and exact formats above.

- [ ] **Step 2: Confirm red**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HistoryViewModelTest" --rerun-tasks --no-daemon
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.haitranduc.fittrack.data.local.dao.StatisticsDaoTest --no-daemon
```

- [ ] **Step 3: Implement final aggregate and summary value**

Add `totalTrainingTimeSeconds: Long = 0L`, extend the repository/History combination, and fill the third summary slot. Preserve history list states and update populated/empty previews.

- [ ] **Step 4: Extend critical UI evidence**

After the existing M3 flow finishes, assert History shows at least one workout/set and a nonnegative formatted time. Add isolated Settings and Favorites navigation/persistence assertions without test-order or developer-database dependence.

- [ ] **Step 5: Run complete finite verification gate**

```powershell
python -m unittest tools/test_prepare_exercise_seed.py -v
.\gradlew.bat :app:clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --no-daemon
.\gradlew.bat connectedDebugAndroidTest --no-daemon
git diff --check
```

Also search main/test sources for `while`, `delay`, `ticker`, and `GlobalScope`. Expected: every command exits zero within 360 seconds, Python has 6 passes, lint has zero errors, and only the lifecycle-bound Compose loop is periodic.

- [ ] **Step 6: Perform APK smoke/persistence checks**

Install the debug APK. Verify favorite survives relaunch, theme survives recreation/relaunch, successful set completion starts a visible 90-second timer that can be skipped, History totals update after Finish, and M3 critical flow has no crash. Record emulator/API and use process/logcat evidence to separate launcher failures from FitTrack failures.

- [ ] **Step 7: Commit only Task 6**

```powershell
git add app/src/main/java/com/haitranduc/fittrack/data/local/dao/StatisticsDao.kt app/src/main/java/com/haitranduc/fittrack/domain/repository/StatisticsRepository.kt app/src/main/java/com/haitranduc/fittrack/data/repository/StatisticsRepositoryImpl.kt app/src/main/java/com/haitranduc/fittrack/presentation/history app/src/main/res/values/strings.xml app/src/test/java/com/haitranduc/fittrack/presentation/history app/src/test/java/com/haitranduc/fittrack/testing/FakeStatisticsRepository.kt app/src/androidTest
git commit -m "feat(statistics): add total training time"
```

Do not create a seventh implementation/fix commit. If the gate exposes a defect, fix it before the owning task commit or stop and report the exact failure.

- [ ] **Step 8: Write external phase report**

Create `../FitTrack_Project_Plan/phase-results/PHASE_4_RESULT.md` with branch, six commits, features/files, migration evidence, exact test commands/counts/durations/timeouts, emulator/API, smoke evidence, lint warnings, preview/accessibility audit, limitations, and PASS/FAIL. Do not mark M4 complete in backlog; Codex does that only after independent review.

## Self-review

- Spec coverage: Task 1 owns timer/anti-hang; Task 2 favorites/migration; Task 3 DataStore theme/Settings; Tasks 4-6 own the three aggregates; Task 6 owns full regression evidence.
- Placeholder scan: each feature has concrete interfaces, SQL/state, tests, commands, files, and one commit boundary.
- Type consistency: exercise IDs are `String`; timestamps/statistics are `Long`; theme uses `ThemePreference`; timer deadlines are epoch milliseconds and UI duration is seconds.
