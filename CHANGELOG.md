# Changelog

## v0.1.2

Final CI/orientation validation pass after independent standalone review.

- Added a JUnit regression test that executes the actual Java `StimulusCatalog.all()` and compares all 289 rows directly with `fixtures/parent_manifest.csv`.
- Kept the independent Python geometry validator, but clarified that it is a separate reconstruction check rather than a substitute for testing the Java catalog.
- Added `python tools/validate_release.py` to both GitHub CI and signed-release workflows.
- Added a runtime landscape check before stereo stimulus presentation, so API-36 tablets/foldables cannot silently run an experiment in portrait if Android ignores the requested orientation.
- Disabled experiment and oscillation touch timing until the posted stimulus-onset clock is initialized.
- Bumped Android version to `versionName 0.1.2`, `versionCode 3`, and updated the release tag default.
- The 289-stimulus mathematical catalog and rendering equations are unchanged from v0.1.1.

## v0.1.1

Android release-integrity pass after independent standalone review.

- Replaced API-36-incompatible reliance on `onBackPressed()` with `OnBackInvokedDispatcher` on API 33+ plus an older-Android fallback.
- Split blind responses into a true free-report-only screen followed by structured scoring.
- Added export of any saved session and export-all ZIP.
- Disabled Android Auto Backup for research data.
- Added explicit experiment `running/completed/aborted` status records and planned/completed counts.
- Added parent-manifest SHA-256 to oscillation records plus oscillation status records.
- Kept the display awake during sustained stimulus viewing.
- Modernized immersive UI to `WindowInsetsController` on API 30+.
- Added `fixtures/parent_manifest.csv` and all-289 catalog validation.
- Updated installed application label to `Stereo Multistability Android`.
- Bumped Android version to `versionName 0.1.1`, `versionCode 2`.
- Added release-workflow validation that GitHub release tag must match Android `versionName`.

## v0.1.0

Initial Android research prototype.
