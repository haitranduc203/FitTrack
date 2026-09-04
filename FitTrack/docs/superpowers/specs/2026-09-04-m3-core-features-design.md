# M3 Core Features Design

## Goal

Replace the M1 presentation mock data with the tested M2 Room/repository layer and deliver FitTrack's complete offline critical flow:

```text
Create Workout -> Add Exercise -> Save -> Start -> Complete Set -> Finish -> History
```

M3 is complete only when exercises, workout templates, the active session, completed sets, and history are driven by persisted data and survive the relevant Activity/process recreation or app restart.

## Scope boundaries

M3 includes:

- application-level exercise seed initialization with explicit loading, ready, and recoverable error states;
- exercise library and exercise detail backed by `ExerciseRepository`;
- workout create/edit/delete with ordered, duplicate-free exercises;
- workout name, exercise-count, reps, weight, start, and finish validation;
- one active workout session at a time;
- immediate persistence of every completed set;
- finishing a valid workout session with timestamps and duration;
- completed-workout history list and snapshot-based history detail;
- ViewModel, use-case, navigation, Compose, repository-extension, and critical-flow tests.

M3 excludes:

- remote APIs, authentication, cloud synchronization, analytics, and Firebase;
- exercise image/GIF downloading or unverified media;
- notifications, settings, DataStore behavior, theme switching, and M4 polish;
- adding a cancel-workout product flow, because it is not an approved P0 requirement;
- changing Room schema version 1 unless a verified M3 requirement is impossible with the existing schema. No destructive migration is allowed.

## Architecture

Keep the existing single `:app` module and Clean Architecture Lite flow:

```text
Stateless Compose content
        ^ state/events
Route composable + Hilt ViewModel
        ^ StateFlow
Domain use case (when a business rule exists)
        ^
Domain repository interface
        ^
M2 repository/DAO/Room
```

Room remains the single source of truth. Presentation code must not import DAOs, Room entities, or M2 repository implementations. Domain use cases and validators must not depend on Android classes or resources.

Each feature screen is split into:

- a route/container composable that obtains a Hilt ViewModel and collects state with `collectAsStateWithLifecycle()`;
- a stateless content composable that receives immutable state and event callbacks;
- at least one deterministic `@Preview` that renders the content composable without Hilt or Room.

Do not keep mutable business state in screen-level `remember`. Text input can be represented in ViewModel state, with `SavedStateHandle` used where restoring unsaved editor/session input is required.

## Startup and seed initialization

M2 deliberately did not start the seed importer. M3 introduces a root `FitTrackViewModel` (or equivalently focused startup ViewModel) that calls `ExerciseRepository.ensureSeeded()` exactly once per ViewModel lifetime.

The root UI exposes:

- `Loading` while seeding;
- `Ready` when either `Imported` or `AlreadySeeded` is returned;
- `Error` with a retry event for a typed seed failure.

The navigation graph is created only after startup is ready. Do not launch an unmanaged coroutine from `Application`, and do not block the main thread.

## Exercise library

`ExerciseListViewModel` owns query, body-part filter, and equipment filter. It observes `ExerciseRepository.observeExercises(query, bodyPart, equipment)` and exposes loading, content, empty, and error states. Filtering remains database-backed through the M2 repository/DAO contract.

The list uses the persisted domain `Exercise.id` and text fields, not Android string-resource mock models. Search and filters combine predictably, and clearing a filter uses `null` for “all”. Exercise detail observes the repository by ID and handles missing/error states without crashing.

`ExerciseMockData` must have no production callers after the exercise task. It may be deleted when previews use explicit domain/sample UI data.

## Workout validation and editor

Business rules are centralized in Android-free domain validation/use-case code:

- trim the workout name before saving;
- name must contain 1 through 50 characters after trimming;
- a workout must contain at least one exercise before Save or Start;
- an exercise may appear only once in a template;
- stored exercise order matches the editor order;
- reps must be 1 through 100 inclusive;
- weight must be 0 through 1000 kg inclusive; zero is valid.

The editor supports create and edit modes. It loads an existing workout by `Long` ID, preserves its `createdAt`, updates `updatedAt`, adds exercises from the persisted library, prevents duplicates, removes exercises, and reorders them using accessible up/down actions. Drag-and-drop is not required.

Saving is explicit. Validation errors remain visible in UI state and no invalid repository write occurs. A successful save returns the real database workout ID and navigates back. Starting from the editor first saves valid changes, then invokes the start-session use case with that saved ID.

Workout deletion is explicit and confirmed. The existing M2 foreign-key behavior must be preserved: deleting a template removes its junction rows and nulls the nullable template reference on sessions while completed history and snapshots remain.

`WorkoutMockData` must have no production callers after workout integration.

## Workout session use cases

Use an injectable Android-free `TimeProvider` so time-dependent behavior is deterministic in unit tests.

`StartWorkoutUseCase`:

- rejects a missing workout;
- rejects a workout with zero exercises;
- rejects starting when an unfinished session already exists;
- otherwise inserts a session with the workout ID/name snapshot and current `startedAt`;
- returns the inserted session ID on success;
- serializes concurrent starts inside the process so two callers cannot both pass the active-session check.

The history repository contract may be extended with focused one-shot/active-session operations needed by these use cases. Keep Room details behind the repository. Prefer reusing the current M2 `getActiveSession` DAO capability and add tests for every new repository method.

`CompleteSetUseCase`:

- accepts the active session, exercise identity/name snapshot, set number, reps, and weight;
- rejects reps outside 1..100 and weight outside 0.0..1000.0;
- rejects a missing or already-finished session;
- persists a valid set immediately with `completedAt` from `TimeProvider`;
- surfaces duplicate/invalid persistence failures without claiming success.

`FinishWorkoutUseCase`:

- rejects a missing or already-finished session;
- rejects a session with no completed sets;
- sets `finishedAt` to the current time;
- stores `durationSeconds = max(0, (finishedAt - startedAt) / 1000)`;
- updates the persisted session and returns its ID.

Use typed sealed results/errors for expected validation, not string matching or exceptions as control flow. Preserve coroutine cancellation.

## Active workout

Change the active-workout destination to carry the persisted session ID:

```text
active_workout/{sessionId}
```

After `StartWorkoutUseCase` succeeds, navigate with the returned ID. The active ViewModel gets `sessionId` from `SavedStateHandle`, observes the session and its persisted sets, and obtains the template exercises through the stored workout ID. It never relies on `WorkoutMockData`.

The UI provides reps and weight input per exercise, an action to complete the next set, validation feedback, the list of completed sets, elapsed time derived from persisted `startedAt`, and a finish action. The displayed timer may tick in memory, but `startedAt`, completed sets, and finish state are persisted; recreation must not reset them.

On successful finish, remove the active screen from the back stack and navigate to the completed history detail (or history tab with a stable way to open the finished session). Back must not return to a finished active session.

## History

`HistoryViewModel` observes finished sessions only. `HistoryDetailViewModel` observes the selected session and its ordered sets. Both expose loading/content/empty-or-missing/error UI states.

History displays persisted snapshot fields (`workoutNameSnapshot`, `exerciseNameSnapshot`, reps, weight, timestamps, and duration). It must remain readable after the original workout template is deleted and after the app is restarted.

`HistoryMockData` must have no production callers after history integration.

## Navigation and identity

Convert workout/session callbacks and route arguments from mock `String` IDs to persisted `Long` IDs. Parse route arguments defensively; an invalid/missing ID produces a missing/error screen and never a crash.

Keep the three top-level destinations and bottom navigation behavior from M1. Detail/editor/active destinations do not show the bottom bar. Existing M1 navigation behavior must be updated to real data rather than deleted.

## Error handling

- Every asynchronous screen exposes a user-visible error state and retry where recovery is possible.
- Do not swallow `DataResult.Failure` or show success before a write succeeds.
- Prevent duplicate taps while save/start/complete/finish/delete is in progress.
- Rethrow coroutine cancellation.
- UI text uses string resources; domain errors contain no Android resource IDs.

## Testing strategy

Use local JVM tests with `kotlinx-coroutines-test` and hand-written fakes for Android-free validators, use cases, and ViewModels. Do not add a mocking framework solely for M3. Use instrumentation tests where real Room, Activity recreation, or Compose navigation/persistence behavior matters.

Required coverage:

- startup imported/already-seeded/failure/retry;
- exercise initial content, search, body filter, equipment filter, combined filter, empty, detail, missing, and error;
- workout blank/overlong/trimmed name, zero exercises, add, duplicate prevention, remove, reorder, save, edit, delete, and error;
- start success, missing workout, empty workout, and existing active session;
- complete set boundaries and invalid values, immediate persistence, and duplicate/failure behavior;
- finish success, no completed set, missing/already-finished session, timestamp, and duration;
- active-workout recreation retains persisted state and does not create a second session;
- history empty/list/detail, finished-only behavior, restart persistence, and template-deletion retention;
- one instrumented critical-flow test covering Create -> Add -> Save -> Start -> Complete -> Finish -> History.

Existing M2 seed, schema, DAO, repository, mapper, and M1 navigation tests remain enabled and passing.

## Commit policy

Planning documentation may have separate `docs:` commits before implementation. Implementation is exactly eight tasks and eight focused commits, in this order:

1. `feat(domain): add workout validation rules`
2. `feat(exercises): connect exercise library to Room`
3. `feat(workouts): implement workout editor`
4. `feat(workouts): implement workout list and deletion`
5. `feat(sessions): implement workout session use cases`
6. `feat(active-workout): implement active workout flow`
7. `feat(history): connect workout history to Room`
8. `test(m3): verify critical workout flow`

Each task follows red-green-refactor: add a failing focused test, run it and record the expected failure, implement the smallest complete behavior, run focused and regression tests, then create only that task's commit. Do not combine tasks, squash, amend across task boundaries, or add a final catch-all implementation commit.

## Completion gate

M3 passes only when:

- the eight implementation commits are separate and ordered;
- no production screen uses `ExerciseMockData`, `WorkoutMockData`, or `HistoryMockData`;
- the critical flow works entirely offline and persists through restart;
- business-rule unit tests and important Room/repository tests pass;
- all content screens still have functional deterministic `@Preview` functions;
- `:app:assembleDebug`, `:app:assembleRelease`, `:app:testDebugUnitTest`, `:app:lintDebug`, and `connectedDebugAndroidTest` pass;
- the debug APK launches on an emulator without a critical-flow crash;
- `git diff --check` is clean and changes stay inside `FitTrack/`.
