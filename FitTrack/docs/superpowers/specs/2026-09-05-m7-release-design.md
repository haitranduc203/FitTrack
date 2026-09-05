# M7 Release Design

## Goal

Prepare FitTrack v1.0.0 as a reproducible educational Android release with accurate documentation, representative screenshots, third-party attribution, known limitations, verified artifacts, and a local annotated Git tag.

## Release scope

### Version metadata

Set `versionName` to `1.0.0` and keep `versionCode` at `1`. Verify the built APK manifest reports these exact values.

### Documentation

Create a root `README.md` that explains the app, features, stack, setup, commands, test strategy, architecture links, screenshots, attribution, limitations, and release artifact path.

Create focused documents:

- `docs/ARCHITECTURE.md`: Mermaid component/data-flow diagram and layer responsibilities.
- `THIRD_PARTY_NOTICES.md`: source and MIT coverage for the exercise metadata/instructions; explicitly state that Gym visual images/GIFs are not bundled.
- `docs/KNOWN_LIMITATIONS.md`: honest v1.0.0 constraints and unsigned-APK status.

### Screenshots

Capture real Pixel 7 Pro AVD screens from the debug app after resetting to a deterministic local state. Include at least:

- exercise library;
- exercise detail;
- workout editor or workout list with content;
- active workout;
- completed history/detail.

Store PNG files under `docs/screenshots/` and inspect each image before committing it. Do not reuse dataset media.

### Security and release gates

Scan tracked files with an installed secret scanner when available and a filename/value-pattern fallback. Never print candidate secret values. Run dataset tests, clean unit/lint/debug/release build, and the full connected suite twice with hard timeouts. Record exact XML counts and artifact hashes.

### Git release state

All M7 work stays on `feature/release`, stacked on `feature/testing`. Create local annotated tag `v1.0.0` only after the final tracked release commit and all gates pass. Do not push or merge.

## Constraints

- Educational, offline-only MVP; no backend, authentication, Firebase, payment, or cloud sync.
- Single-module MVVM + UDF + Room architecture remains unchanged.
- No new runtime dependency.
- One coherent task per commit.
- No signing secret or keystore is created or committed.
- The release APK remains unsigned; documentation must say so.
- Exercise metadata and English instructions are derived from `hasaneyldrm/exercises-dataset` under MIT.
- Gym visual media is excluded because cloning the source dataset does not grant reuse rights.
- Device test commands have a 360-second hard timeout and must never be allowed to hang indefinitely.

## Completion criteria

- All M7 backlog rows are complete.
- `versionName=1.0.0`, `versionCode=1` is verified from the APK.
- Documentation links and screenshot files resolve.
- Dataset/unit/connected tests pass, lint has zero errors, and release APK builds.
- Secret scan reports no committed credential or private key.
- `git diff --check` and repository status are clean.
- Local annotated tag `v1.0.0` points to the verified release commit.
