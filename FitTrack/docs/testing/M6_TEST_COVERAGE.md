# M6 Test Coverage Matrix

This matrix maps the M6 backlog and `FitTrack_Project_Plan/09_TEST_PLAN.md` to executable behavioral tests. Tests created during earlier milestones remain valid evidence; M6 does not duplicate them.

## Backlog traceability

| M6 backlog item | Primary evidence | Status |
|---|---|---|
| Workout validation | `WorkoutValidationTest`: `validateName_blankOrWhitespace_returnsBlank`, `validateName_validLengths_returnsValidWithTrimmedName`, `validateExerciseIds_emptyList_returnsEmpty`, `validateExerciseIds_duplicateIds_returnsDuplicateWithFirstDuplicatedId`; `SaveWorkoutUseCaseTest` covers the combined save contract. | Covered |
| `CompleteSetUseCase` | `CompleteSetUseCaseTest`: exact persisted snapshot, boundary values, invalid reps/weight without writes, missing/finished session, repository errors, cancellation. | Covered |
| `FinishWorkoutUseCase` | `FinishWorkoutUseCaseTest`: exact `finishedAt` and floored duration, no-set rejection, missing/already-finished session, second finish, repository errors, negative-duration clamp, cancellation. | Covered |
| `ExerciseDao` | `ExerciseDaoTest`: insert/count, ID lookup, query-all ordering, body-part/equipment/search/combined filters. | Covered |
| `WorkoutDao` | `WorkoutDaoTest`: create/update/delete, ordered exercise relation, unique/foreign-key constraints, transaction rollback. | Covered |
| `WorkoutSessionDao` | `WorkoutHistoryDaoTest`: active lookup, finish update, finished-history ordering, sets relation, cascades, file-backed reopen persistence, template deletion preserving snapshots. | Covered |
| `ExerciseListViewModel` | `ExerciseListViewModelTest`: initial/success/search/body/equipment/combined/empty/error/retry/favorites, saved filters, deduplication, debounce cancellation. | Covered |
| `WorkoutEditorViewModel` | `WorkoutEditorViewModelTest`: create/edit/missing, name/add/duplicate/remove/reorder, invalid save, success/failure/double action, start outcomes, unsaved state and recreation. | Covered |
| `ActiveWorkoutViewModel` | `ActiveWorkoutViewModelTest`: load/missing, elapsed time, input, invalid/success/failure complete-set, finish outcomes, duplicate finish, rest timer and saved-state restoration. | Covered |
| `HistoryViewModel` | `HistoryViewModelTest`: empty/success/error/retry/retained content and all statistics; `FitTrackNavigationTest.test_historyList_to_historyDetail_andBack` covers the open-detail UI boundary. | Covered |
| Critical Compose flow | `M3CriticalFlowTest.criticalWorkoutFlow_create_start_completeSet_finish_persistsAcrossRecreate_survivesTemplateDeletion`. | Covered |
| Regression checklist | Matrix below, including the M6 Compose invalid-input test. | Covered |

## Regression checklist

| Check | Evidence | Status |
|---|---|---|
| Navigation | `test_bottomNavigationDestinations`, exercise/detail, workout/editor/active, and history/detail navigation tests. | Covered |
| Configuration changes | Filter, editor draft, Active Workout input/timer, theme, Snackbar non-replay tests in `FitTrackNavigationTest`. | Covered |
| Dark theme | `M3CriticalFlowTest.themeSelection_survivesActivityRecreation` and settings recreation test. | Covered |
| Offline | App has no remote data source; Room seed, repository integration, and file-backed reopen tests exercise local operation. | Covered by architecture/integration |
| Empty exercise search | `test_searchNonMatchingQuery_showsEmptyState` plus `ExerciseListViewModelTest.emptyResult_showsEmptyListWithoutError`. | Covered |
| Empty workout list | `WorkoutListViewModelTest.emptyWorkouts_showsEmptyState` and empty-state Compose content coverage. | Covered |
| Empty history | `HistoryViewModelTest.observeHistory_empty_showsEmptySessions` and empty-state Compose content coverage. | Covered |
| Invalid reps | Validation/use-case/ViewModel tests plus `test_activeWorkout_invalidRepsAndWeight_showErrorsWithoutPersistingSet`. | Covered |
| Invalid weight | Validation/use-case/ViewModel tests plus `test_activeWorkout_invalidRepsAndWeight_showErrorsWithoutPersistingSet`. | Covered |
| Multiple active workout prevention | `StartWorkoutUseCaseTest` conflict/concurrency cases and `test_activeWorkout_backAndResume_resumesExistingSessionWithoutDuplicate`. | Covered |
| Delete workout keeps history | `WorkoutHistoryDaoTest.deletingWorkoutTemplate_preservesCompletedHistoryAndSetsSnapshots` and the critical flow. | Covered |
| App restart preserves data | File-backed Room reopen test and critical-flow activity recreation with history snapshots. | Covered |

## Final verification

Verified on 2026-09-05:

- Dataset tools: 12 tests, 0 failures (`1.173s`).
- Clean host gate: exit 0 (`70.1s`).
- JVM unit tests: 194 tests, 0 failures, 0 errors, 0 skipped.
- Lint: 121 issues, 0 errors.
- Debug APK: 14,246,160 bytes.
- Release unsigned APK: 9,682,326 bytes.
- Connected run 1: 92 tests, 0 failures, 0 errors, 0 skipped (`140.4s`).
- Connected run 2: 92 tests, 0 failures, 0 errors, 0 skipped (`151.7s`).
- `git diff --check`: clean.

The connected suite increased from 91 to 92 tests. Both full runs completed below the 360-second limit. The SDK XML compatibility warning remains non-blocking.
