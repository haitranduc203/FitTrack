# Known Limitations — v1.0.0

FitTrack v1.0.0 is an educational, offline-first MVP. The following constraints are intentional and describe the current release.

## Data and devices

- All workouts, sessions, favorites, and settings are stored only on the current Android device.
- There is no account, cloud synchronization, multi-device transfer, export, or import workflow.
- Clearing app storage or uninstalling the app can remove locally stored user data.
- The app supports one active workout session at a time.

## Exercise content

- The exercise library contains 1,324 records with English names/instructions and metadata derived from the attributed dataset.
- Exercise thumbnails and animation GIFs are intentionally not bundled because the upstream Gym visual media has separate reuse terms.
- The app does not provide coaching, form analysis, personalized recommendations, or medical guidance.

## Workout behavior

- The rest timer is an in-app visual timer. It does not schedule an Android notification, alarm, or foreground service.
- Statistics are intentionally simple: total workouts, completed sets, and recorded training time.
- Weight is stored in kilograms; unit conversion is not available.

## Distribution

- `assembleRelease` produces `app-release-unsigned.apk`.
- The APK is suitable for local verification but must be signed with a private release key before normal distribution.
- No signing key, keystore, password, or store credential is included in this repository.
- Store listing, Play Console configuration, privacy-policy hosting, and production crash reporting are outside the educational project scope.

## Platform and language

- Minimum Android version is API 24; target and compile SDK are API 36.
- User-facing copy is English-only in v1.0.0.
- Tablet-specific and foldable-specific layouts are not separately optimized, although Compose layouts use standard responsive constraints.
