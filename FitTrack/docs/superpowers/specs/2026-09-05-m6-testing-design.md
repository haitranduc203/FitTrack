# M6 Testing Design

## Goal

Close every M6 backlog item with traceable behavioral evidence while adding only tests that catch a real uncovered regression.

## Current evidence

The branch already contains focused suites for workout validation, `CompleteSetUseCase`, `FinishWorkoutUseCase`, `ExerciseDao`, `WorkoutDao`, workout-session/history DAOs, the four required ViewModels, and the end-to-end critical workout flow. These suites were created during M2–M5 and remain valid M6 evidence; M6 must not duplicate them just to create new commits.

The coverage audit identified one material gap in the regression checklist: invalid reps and invalid weight are covered at validation, use-case, and ViewModel levels, but not through the real Compose input flow. M6 will add that one connected regression scenario.

## Design

### Coverage traceability

Create `docs/testing/M6_TEST_COVERAGE.md` as a concise matrix. Each backlog item maps to exact test classes and behavioral test names. The matrix distinguishes pre-existing evidence from M6 additions and records the final commands and counts.

### Compose regression

Extend `FitTrackNavigationTest` with one finite end-to-end scenario:

1. Create and start a workout containing exercise `0001`.
2. Submit reps `0` with valid weight and assert the reps validation message.
3. Submit valid reps with weight `-1` and assert the weight validation message.
4. Confirm the active session has no persisted set logs.
5. Clean up the active session and temporary workout in `finally`.

The test uses the stable Active Workout field tags introduced in M5. It uses `waitUntil` with explicit timeouts and performs no real-time sleep or unbounded scheduler advancement.

### Regression and quality gates

M6 is complete only after:

- dataset tool tests pass;
- all JVM unit tests pass;
- lint reports zero errors;
- debug and release APKs build;
- the full connected suite passes twice consecutively on a healthy AVD;
- `git diff --check` is clean;
- the worktree contains no generated artifacts.

## Constraints

- Keep the existing single-module MVVM/UDF/Room architecture.
- Add no dependency and no production business behavior.
- Do not add duplicate tests for already-covered branches.
- One coherent task per commit.
- Never use `Thread.sleep`, `GlobalScope`, unbounded `while` loops in tests, or `advanceUntilIdle` around periodic timer work.
- Connected commands have a 360-second hard timeout; UI waits have explicit finite timeouts.
- Do not push, merge, or tag remote state.

## Out of scope

- Coverage-percentage targets.
- Benchmark infrastructure.
- New features or refactoring unrelated to testability.
- Release documentation and screenshots, which belong to M7.
