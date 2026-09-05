# M4 Secondary Features Design

## Goal

Add the four approved secondary capabilities after the M3 critical flow: a rest timer, persistent favorite exercises, a persistent theme setting, and simple lifetime training statistics.

M4 must preserve the offline-first architecture, Room history guarantees, existing navigation flow, deterministic previews, and all M0-M3 tests.

## Scope boundaries

M4 includes:

- a fixed 90-second rest countdown started only after a set is persisted successfully;
- persistent favorite/unfavorite actions from the exercise list and detail;
- a Favorites filter in the exercise list;
- a Settings destination with System, Light, and Dark theme choices persisted in Preferences DataStore;
- total completed workouts, total completed sets, and total training time shown above the History list;
- Room migration 1 to 2 for favorite metadata, without destructive fallback;
- focused unit, Room migration/DAO/repository, Compose/navigation, and regression tests.

M4 excludes:

- configurable rest duration, sound, vibration, notifications, or background services;
- cloud sync, accounts, analytics, social features, and remote APIs;
- editing canonical exercise seed data;
- charts, weekly/monthly trends, goals, streaks, calories, and body metrics;
- broad M5 polish unrelated to these features.

## Architecture

Keep the single `:app` module and current Clean Architecture Lite boundaries:

```text
Compose content
    <- immutable UI state / events
Hilt ViewModel
    <- domain repository interfaces
Room or Preferences DataStore implementation
```

Room remains the source of truth for favorites and training history. Preferences DataStore is the source of truth only for theme preference. Domain code remains Android-free; Compose code must not access DAOs, entities, or DataStore directly.

All asynchronous screens expose localized loading/error/retry behavior where recovery is possible. Every modified content composable keeps deterministic `@Preview` coverage without requiring Hilt, Room, or DataStore.

## Rest timer

The rest timer belongs to the active-workout presentation state. It uses a fixed `REST_DURATION_SECONDS = 90` for M4.

After `CompleteSetUseCase` returns `Success`, the ViewModel stores a deadline (`restTimerEndsAtMillis`) derived from the injected `TimeProvider`. The UI shows remaining `mm:ss`, a progress indicator, and a Skip action. Completing another set restarts the countdown from 90 seconds. Skip or reaching zero clears the timer.

The deadline is stored through `SavedStateHandle`, so Activity/configuration recreation recomputes the remaining time instead of restarting it. A cold app restart or background notification is not guaranteed in M4.

The existing lifecycle-aware timer effect in `ActiveWorkoutScreen` remains the only periodic coroutine. Its tick calls a synchronous ViewModel method that updates both workout elapsed time and rest time from timestamps.

Hard anti-hang constraint:

- never place `while`, periodic `delay`, `ticker`, or a self-rescheduling coroutine in a ViewModel, use case, repository, fake, or unit test;
- do not run `advanceUntilIdle()` while a perpetual periodic job exists on the test scheduler;
- a Compose `LaunchedEffect` loop is allowed only while lifecycle-bound, cancellable, and calling synchronous `onTimerTick()`;
- focused timer tests call `onTimerTick()` manually with `FakeTimeProvider` and must finish under a command timeout.

## Favorite exercises

Favorites are user metadata, not a modification to the canonical seeded exercise rows. Add a `favorite_exercises` table with `exerciseId` as its primary key, a foreign key to `exercises(id)` using `ON DELETE CASCADE`, and `createdAt` for deterministic ordering/debugging.

Upgrade `FitTrackDatabase` from version 1 to 2 with an explicit `MIGRATION_1_2` that creates only this table and its required constraint/index. Register the migration in `DatabaseModule`. Never use `fallbackToDestructiveMigration`; existing workouts, sessions, set logs, and seeded exercises must survive migration.

Add a focused favorite DAO and repository boundary. The exercise repository continues to provide canonical exercise data; a favorite repository exposes the favorite ID set and an idempotent `setFavorite(exerciseId, isFavorite)` operation. Presentation state combines exercise results with the favorite ID set.

The exercise list provides:

- a localized favorite toggle on each exercise row;
- a Favorites-only filter that composes with search, body-part, and equipment filters;
- visible localized failure feedback if a toggle or observation fails;
- duplicate-tap protection while an exercise toggle is being persisted.

Exercise detail provides the same toggle and reflects changes immediately through Room observation. Favorites persist after database close/reopen and normal app restart.

## Theme setting

Use the already-declared `androidx.datastore:datastore-preferences` dependency. Store one enum-like preference with exact values `SYSTEM`, `LIGHT`, and `DARK`; unknown/corrupt values fall back to `SYSTEM`.

Add a settings repository interface and Preferences DataStore implementation. A root theme ViewModel exposes the preference before rendering `FitTrackTheme`, using system dark mode only when the saved value is `SYSTEM`.

Add `Settings` as a fourth top-level bottom-navigation destination. `SettingsScreen` presents three single-choice theme options and applies changes immediately after DataStore confirms the write. Save failures remain visible and do not claim a successful selection. The screen includes loading/error/retry/content states and deterministic previews.

Theme choice must survive Activity recreation and app restart. M4 does not add unrelated settings.

## Training statistics

Show a compact statistics summary at the top of the existing History screen. Do not add a separate analytics screen or chart library.

Statistics use only finished sessions (`finishedAt IS NOT NULL`):

- total workouts: count of finished workout sessions;
- total completed sets: count of set logs whose owning session is finished;
- total training time: sum of non-null `durationSeconds` from finished sessions, clamped to a nonnegative value and formatted as hours/minutes in presentation code.

Add a dedicated `StatisticsRepository` rather than expanding presentation code with Room access. Each aggregate is exposed as a reactive `Flow<DataResult<...>>`, so finishing a workout updates History without manual refresh. SQL performs aggregation; do not load all history solely to calculate totals in Kotlin.

Empty history produces zero for every statistic. SQL/repository failures map to localized UI errors and retain retry behavior. Existing history list/detail snapshot behavior remains unchanged.

## Navigation and UI behavior

The top-level destinations become Exercises, Workouts, History, and Settings. Preserve `launchSingleTop`, state restoration, and start-destination behavior. Detail/editor/active/history-detail screens continue hiding the bottom bar.

All new icons have localized content descriptions, touch targets remain accessible, and selected state is exposed semantically. User-visible text belongs in string resources.

## Testing strategy

Use local JVM tests with `kotlinx-coroutines-test`, hand-written fakes, `FakeTimeProvider`, and direct synchronous timer ticks. Use Android instrumentation for Room migration, real DataStore persistence, navigation, Activity recreation, and database close/reopen behavior.

Required coverage:

- rest timer starts only after successful set persistence, restarts on another success, counts down from timestamps, skips, reaches zero, and restores from `SavedStateHandle`;
- timer tests finish without an infinite scheduler job;
- migration 1 to 2 preserves all existing tables/data and creates usable favorites;
- favorite insert/remove/idempotency, list/detail synchronization, combined Favorites/search/body/equipment filtering, failure handling, and restart persistence;
- theme default/System behavior, Light/Dark writes, invalid stored-value fallback, write failure, immediate application, recreation, and restart persistence;
- each statistic SQL aggregate, zero state, finished-only semantics, reactive update, overflow-safe duration sum, repository failure, and formatting;
- four-destination navigation plus unchanged M3 critical flow;
- all existing JVM and instrumented tests remain enabled and passing.

Every Gradle or test process must have an external finite timeout. If a focused test exceeds its timeout, stop the process and investigate the active coroutine/job; never wait indefinitely or hide the failure by deleting/disabling the test.

## Commit policy

Planning documentation may use separate `docs:` commits. M4 implementation is exactly six tasks and six ordered implementation commits:

1. `feat(active-workout): add rest timer`
2. `feat(exercises): add persistent favorites`
3. `feat(settings): add theme preference`
4. `feat(statistics): add total workouts`
5. `feat(statistics): add total completed sets`
6. `feat(statistics): add total training time`

Each task follows red-green-refactor: add a focused failing test, run it and confirm the intended failure, implement the smallest coherent behavior, run focused and relevant regressions, then commit only that task. Do not squash, amend across task boundaries, or add a catch-all implementation commit.

## Completion gate

M4 passes only when:

- the six implementation commits are separate and ordered;
- Room schema version 2 has an explicit tested 1-to-2 migration and no destructive migration;
- rest timer, favorites, theme, and all three statistics work offline and meet the persistence rules above;
- no periodic coroutine exists in a ViewModel, use case, repository, fake, or unit test;
- all modified/new content screens retain deterministic previews;
- `:app:assembleDebug`, `:app:assembleRelease`, `:app:testDebugUnitTest`, `:app:lintDebug`, and `connectedDebugAndroidTest` pass within finite timeouts;
- the debug APK launches and the M3 critical flow still works;
- `git diff --check` is clean and changes stay inside `FitTrack/`;
- `../FitTrack_Project_Plan/phase-results/PHASE_4_RESULT.md` records exact commands, counts, migration evidence, emulator/API, warnings, known limitations, and PASS/FAIL without being staged in the FitTrack Git history.
