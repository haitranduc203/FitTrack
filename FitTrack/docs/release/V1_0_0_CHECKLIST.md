# FitTrack v1.0.0 Release Checklist

- **Release branch:** `feature/release`
- **Verified date:** 2026-09-05
- **Application ID:** `com.haitranduc.fittrack`
- **Version:** `versionName=1.0.0`, `versionCode=1`
- **Release type:** Educational, local, unsigned APK

## Source and repository hygiene

- [x] Only the `FitTrack/` project folder is tracked for application work.
- [x] `git diff --check` returns no errors.
- [x] Generated build outputs and local SDK configuration are ignored.
- [x] No signing keystore or signing credential is tracked.
- [x] M5, M6, and M7 work is split into milestone branches and atomic commits.

## Secret scan

`gitleaks` was not installed, so the tracked tree was checked with two local fallbacks that do not print candidate values:

- Suspicious credential/private-key filenames: **0**.
- Files matching high-confidence Google, AWS, GitHub, Stripe, Slack, or private-key patterns: **0**.
- Tracked `local.properties`: **0**.

This scan is suitable for the current offline educational project; no credential is required by the app.

## Dataset verification

```text
python -m unittest discover -s tools -p "test_*.py" -v
12 tests, 0 failures — 1.108s
```

- [x] Packaged seed contains exactly 1,324 records.
- [x] Source and bundled dataset license SHA-256 values match.
- [x] No upstream `image`, `gif_url`, `media_id`, or `attribution` field is present in the packaged seed.
- [x] Dataset source/license and media exclusion are documented in `THIRD_PARTY_NOTICES.md`.

## Clean host gate

```text
.\gradlew.bat :app:clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --no-daemon
Exit code: 0 — 88.0s
```

- JVM tests: **194**, failures **0**, errors **0**, skipped **0**.
- Lint: **121 issues**, **0 errors**.
- Debug build: passed.
- Release build: passed.

The Android tooling emitted a non-blocking warning about SDK XML version compatibility.

## Connected gate

Pixel 7 Pro AVD, API 34:

| Run | Tests | Failures | Errors | Skipped | Wall time |
|---|---:|---:|---:|---:|---:|
| 1 | 92 | 0 | 0 | 0 | 163.4s |
| 2 | 92 | 0 | 0 | 0 | 153.6s |

- [x] Two consecutive full runs passed.
- [x] Both runs completed below the 360-second hard timeout.
- [x] No unbounded coroutine scheduler advancement was introduced.

## APK evidence

`aapt dump badging` reports:

```text
package: name='com.haitranduc.fittrack' versionCode='1' versionName='1.0.0'
```

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 14,246,148 | `53BECAC5212D554F8FB93EECE9B546B3A6FAACB4C46853F2D05A21DBCDB6A3A1` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 9,682,326 | `4E2D3410959D6CB992AC8BB929B03807964ACCEC37FCC1CAAF210621947375F8` |

The release APK is unsigned. Signing and store distribution require a separately protected release key and are not performed by this repository.

## Documentation and screenshots

- [x] Root README with build/test instructions and honest release status.
- [x] Mermaid architecture diagrams.
- [x] Dataset attribution and bundled license.
- [x] Known limitations.
- [x] Five real emulator screenshots.
- [x] README relative-link targets exist.
- [x] Every screenshot is a valid 1440×3120 PNG and was visually inspected for loading states, dialogs, keyboard overlays, and clipping.

## Release decision

FitTrack v1.0.0 meets the project plan's educational release criteria. The verified release artifact is the unsigned APK above; it is not represented as a Play Store-ready binary.
