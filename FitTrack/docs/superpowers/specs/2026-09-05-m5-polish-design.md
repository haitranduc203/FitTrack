# M5 Polish Design Specification

## 1. Goal

Refine, harden, and polish the FitTrack Android application across all 10 application screens, delivering a coherent, accessible, dark-theme-consistent, offline-first user experience with robust state handling (loading, empty, error, retry, transient snackbars, and destructive confirmations), lifecycle-safe configuration persistence, and audited 1,324-exercise list rendering performance.

## 2. Scope Boundaries

### In Scope
- **10 Core Screens Audited:**
  1. Startup Screen (`FitTrackNavHostContent`)
  2. Exercise List Screen (`ExerciseListScreen`)
  3. Exercise Detail Screen (`ExerciseDetailScreen`)
  4. Workout List Screen (`WorkoutListScreen`)
  5. Workout Editor Screen (`WorkoutEditorScreen`)
  6. Exercise Picker Sheet (`ExercisePickerDialog`)
  7. Active Workout Screen (`ActiveWorkoutScreen`)
  8. History Screen (`HistoryScreen`)
  9. History Detail Screen (`HistoryDetailScreen`)
  10. Settings Screen (`SettingsScreen`)
- **10 Polish Focus Areas:**
  - Task 1: Loading states & action debouncing (prevent double-submits, retain existing data during reload).
  - Task 2: Granular empty states (distinct messages for empty library, search no match, empty favorites, empty workouts, empty history, missing exercise, missing workout, missing session).
  - Task 3: Error & retry states (localized messages, clear retry actions, non-swallowed `CancellationException`/`Error`, no permanent loading/saving lockup).
  - Task 4: Transient feedback via Snackbar (one-shot events consumed once, safe across recreation, no boolean state).
  - Task 5: Destructive confirmations (AlertDialog for workout deletion and unsaved editor discard; double-submit prevention on finish workout).
  - Task 6: Material 3 Dark theme consistency (semantic color tokens, no raw hardcoded hex/Color constants, light & dark previews).
  - Task 7: Accessibility & Semantics (min 48dp touch targets, clear `contentDescription`, `heading` semantics, selection state).
  - Task 8: Configuration recreation (`SavedStateHandle`/`rememberSaveable` state preservation, verified via `ActivityScenario.recreate()`).
  - Task 9: Exercise list rendering performance (stable keys, `contentType`, `distinctUntilChanged` on filter flow, fast string search).
  - Task 10: Clean code & dead dependency cleanup (remove unused imports, dead helpers, zero functional regression).

### Out of Scope
- No remote APIs, accounts, cloud sync, or Firebase.
- No modifications to canonical exercise dataset schema or raw dataset JSON.
- No redesign or replacement of the established Material 3 design system.
- No multi-module refactoring.

---

## 3. Screen State Audit Matrix

| Screen | Loading State | Content / Success | Empty / Missing State | Error State & Retry | Transient Feedback (Snackbar) | Destructive Confirmation | Dark Theme Verification | Accessibility & Semantics |
|---|---|---|---|---|---|---|---|---|
| **Startup** | Centered `CircularProgressIndicator` + `loading_startup` | Transitions to `Ready` (TopLevel navigation) | N/A (dataset seeded) | Error card with message + Retry button | N/A | N/A | Dark surface & text tokens | Semantic heading, progress accessibility |
| **Exercise List** | Full screen on first load; preserves list on refresh | `LazyColumn` of exercise cards with body part, equipment, target chips | Differentiates: (a) No exercises in DB; (b) Search/filter no match; (c) Empty favorites | Error card + Retry button; favorite toggle error banner | Snackbar on favorite toggle error / status | N/A | Dynamic theme cards & chip containers | 48dp star touch target, chip selected semantics, search label |
| **Exercise Detail** | Centered `CircularProgressIndicator` | Metadata cards, chips, scrollable instruction list | Missing exercise ID: explicit error card + Back button (replaces generic empty text) | Error text + Retry button | Snackbar on favorite toggle failure | N/A | SurfaceVariant backgrounds, onSurface text | Star icon contentDescription, back icon description |
| **Workout List** | Full screen on first load; keeps workouts on reload | `LazyColumn` of workout template cards with exercise summary chips | Empty templates: card with `empty_workouts` message | Error card + Retry button | Snackbar on workout template deletion success | `AlertDialog` with Confirm ("Delete") & Cancel, disabled while deleting | Surface & elevation tokens, container colors | FAB description, delete button description, dialog semantics |
| **Workout Editor** | Full screen on initial template load; `isSaving` on Save | Name input, exercise reorder/remove list, FAB add | Missing template ID: error card with Back button | Inline error card for validation / database failure | Snackbar on successful save / validation alert | Confirmation dialog when navigating back with unsaved edits | OutlinedTextField colors, card elevation | Move up/down button descriptions, text field labels |
| **Exercise Picker** | Responsive list | Filtered exercise items in `LazyColumn` | No matching exercises in search query | N/A (in-memory filtering) | N/A | N/A | ModalBottomSheet container and card colors | Close icon description, search field label |
| **Active Workout** | Full screen on session load; `isCompletingSet` & `isFinishing` | Elapsed time banner, 90s rest timer banner, exercise set cards | Missing session ID: explicit error card + Back button (fixes infinite loading bug) | Set error text, finish error text, retry | Snackbar on invalid set inputs or complete set failure | Finish workout double-tap guard (`isFinishing`) | Rest timer progress bar & secondaryContainer tokens | Check button description, skip rest button description |
| **History** | Full screen on initial load | Statistics banner (3 slots) + completed session cards | Empty history: card with `empty_history` message | Error card + Retry button | N/A | N/A | SurfaceVariant statistics container, primary text | Statistics labels, card click semantics |
| **History Detail** | Centered `CircularProgressIndicator` | Summary card, duration, timestamp, logged sets | Missing session ID: explicit error card + Back button (replaces generic text) | Error card + Retry button | N/A | N/A | Surface elevation, container colors | Back icon description, set row layout |
| **Settings** | Non-blocking (DataStore is fast/cached) | Theme selection radio group (System, Light, Dark) | N/A | Error card with Retry button on read failure | Snackbar on theme write failure | N/A | Contrast in both Light and Dark modes | Radio button selection semantics, section heading |

---

## 4. Key Architectural Patterns for M5

### 4.1 One-Shot Snackbar Event Handling
To prevent repeated Snackbar display upon configuration changes or Activity recreation:
- Use a Kotlin `Channel<UiText>(Channel.BUFFERED)` exposed as `Flow<UiText>` in ViewModels.
- In Compose, collect with `LaunchedEffect(Unit)` or `collectWithLifecycle`, showing via `SnackbarHostState.showSnackbar(message)`.
- Channels ensure each event is delivered to exactly one observer and consumed once.
- Zero boolean state flags for snackbars.

### 4.2 Destructive Action Confirmations
- **Workout Deletion:** Retains `AlertDialog` in `WorkoutListScreen`:
  - Title: `R.string.dialog_delete_workout_title`
  - Body: `R.string.dialog_delete_workout_message`
  - Confirm: `R.string.action_delete` (disabled if `isDeleting`)
  - Cancel: `R.string.action_cancel`
- **Workout Editor Discard:** In `WorkoutEditorScreen`, when user taps Back or system back while `hasUnsavedChanges` is true:
  - Title: `R.string.dialog_discard_changes_title`
  - Body: `R.string.dialog_discard_changes_message`
  - Confirm: `R.string.action_discard`
  - Cancel: `R.string.action_keep_editing`
- **Finish Workout:** In `ActiveWorkoutScreen`, `onFinishClicked()` guards with `if (uiState.isFinishing) return`, and the Button has `enabled = !uiState.isFinishing`. Finishing persists history (constructive action) and prevents double submissions.

### 4.3 Missing Entity State vs Empty State
- **Empty State:** A valid collection query returned 0 items (e.g., 0 templates created, 0 sessions logged, search returned 0 matches). Displayed in-place with guidance.
- **Missing Entity State:** A detail route with a specific ID (`exerciseId`, `workoutId`, `sessionId`) was requested but does not exist in Room. Displayed as an error card with an explicit "Navigate Back" button, never confusing user with "No items".

### 4.4 Material 3 Semantic Coloring
- Replace any hardcoded alpha/colors with standard `MaterialTheme.colorScheme` tokens:
  - Text: `onSurface`, `onSurfaceVariant`, `onErrorContainer`, `onPrimary`
  - Containers: `surface`, `surfaceVariant`, `primaryContainer`, `secondaryContainer`, `errorContainer`
  - Dividers/Outlines: `outline`, `outlineVariant`
- Validate that all `@Preview` composables have both Light and Dark mode variations.

### 4.5 Performance Optimizations for Exercise List
- `items(uiState.exercises, key = { it.id }, contentType = { "exercise_item" })`
- `distinctUntilChanged()` on filter parameter flows in `ExerciseListViewModel`.
- In-memory search filtering preserves fast indexed lookup without allocating unnecessary intermediate collections.
