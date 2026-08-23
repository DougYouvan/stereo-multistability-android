# Stereo Multistability Android

A landscape-only Android companion to the public **Stereo Multistability Atlas**.

Parent research repository:
https://github.com/DougYouvan/stereo-multistability-atlas

This app does **not** use a headset or stereoscopic viewer. The phone is held sideways in landscape orientation and presents ordinary side-by-side free-fusion pairs.

## What is in v0.1.2

- **289 exact atlas stimuli**, generated mathematically on the phone rather than stored as image files.
- **Atlas mode**: pair only; Volume Up = next, Volume Down = previous.
- **Blind experiment mode**: 40 trials, exact zero-disparity control, balanced signs, eight hidden repeats, minimum repeat lag of eight trials, per-trial checkpointing, and explicit completed/aborted status records.
- **Genuinely two-stage reporting**: stimulus → free report only → structured scoring. The structured vocabulary is not shown until a nonempty free report is submitted.
- **Parallel/wall-eyed or crossed fusion** selection.
- **Oscillation mode**: the fixed pair remains alone on screen. Tap left third = cube, center third = top stack, right third = front stack; long-press = other. Events are timestamped and include parent-manifest provenance.
- **Continuous disparity explorer**: continuously scan global disparity from 0 to 6.5 and select each of the eight local vertex-perturbation patterns.
- **Manual physical calibration** using the 85.60-mm width of a standard ID-1 payment/identity card.
- **Saved-session recovery**: export any retained session CSV, or export all saved session CSVs as one ZIP.
- **Android 16 predictive Back support** using `OnBackInvokedDispatcher` for API 33+; older Android versions retain the legacy fallback.
- **Modern immersive mode** using `WindowInsetsController` on API 30+.
- **Screen-awake protection** during atlas, experiment-stimulus, oscillation, and continuous-explorer viewing.
- **Android Auto Backup disabled** for research data.
- **No Internet permission and no storage permission**.

The controlled stimulus screen is deliberately minimal: **one stereo pair and nothing else**.
 Touch/gesture scoring remains disabled until the posted stimulus-onset clock has initialized, preventing a pre-clock timing race.

## Exact geometry and parent-manifest fixture

The Android implementation reproduces the parent repository's v0.2.3 construction:

- canonical image: 900 × 450
- model-to-image scale: 0.52
- panel centers: x = 285 and 615
- baseline yaw: 6 degrees
- explicit rotation origin: x = 0
- depth seed: `[-100,-100,-100,-100,+100,+100,+100,+100]`
- one zero-disparity control
- 18 nonzero global scales × 8 local patterns × 2 signs = 288 experimental stimuli

Total: **289**.

Parent v0.2.3 manifest SHA-256: `51cf147a8c8cf8016795cfcccefa63cdb058612bb3fcb8f5166eface8ebaec52`.

`fixtures/parent_manifest.csv` contains the 289 parent stimulus IDs and parameter quadruples. Two complementary checks are used:

```bash
python tools/validate_geometry.py
```

performs an independent Python reconstruction/geometry fixture check, while the JUnit test `actualJavaCatalogMatchesAll289ParentFixtureRows()` executes the **actual `StimulusCatalog.all()` Java code** and compares all 289 rows directly against the same fixture. Both run in GitHub CI. `tools/validate_release.py` is also enforced in both CI and release workflows.

## Install directly from GitHub

The intended distribution path is:

`GitHub source → GitHub Actions → signed APK → GitHub Release → Android phone`

You do **not** need Google Play to distribute this research app.

### One-time signing setup

Android requires every installable APK to be signed. A permanent signing key is necessary so later releases install as updates to the same app.

On Windows with a JDK installed:

```powershell
powershell -ExecutionPolicy Bypass -File tools/create-signing-key.ps1
```

Back up the `.jks` file privately and never commit it. Add these GitHub repository secrets under **Settings → Secrets and variables → Actions**:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

### Build the installable APK entirely on GitHub

1. Open **Actions**.
2. Choose **Build signed APK and publish release**.
3. Click **Run workflow**.
4. For this release use `v0.1.2`.
5. The workflow refuses to release if the requested tag does not equal `v` + the APK's `versionName`.
6. Open **Releases** and download `stereo-multistability-android-v0.1.2.apk` on the phone.
7. Android may ask you to allow installation from the browser/file manager used to open the APK.

The project pins Gradle 9.5.0 in GitHub Actions. Before any future release, update both `versionName` **and** the monotonically increasing Android `versionCode`; the release workflow verifies the tag/versionName match.

## Phone orientation and fusion

The app requests **landscape** orientation and also verifies at runtime that the active display is wider than it is tall before entering atlas, experiment, oscillation, or continuous-stimulus viewing. This second check matters on Android 16 / API 36 large-screen devices, where the operating system can ignore an application's requested orientation. If the display is portrait, the app stops and asks the user to rotate before proceeding. No Cardboard-style viewer is assumed or recommended.

Choose:

- **Parallel / wall-eyed** — ordinary left panel on the left, right panel on the right.
- **Crossed** — panel order is swapped.

The convention is recorded in experiment and oscillation CSV files.

## Data integrity and privacy

Every completed trial is synchronously checkpointed to private app storage. Experiment files also contain explicit status records (`running`, `completed`, or `aborted`), planned/completed trial counts, session UUID, seed, participant code, and parent-manifest SHA-256. Oscillation runs likewise contain a run UUID, status records, and the parent-manifest SHA-256.

The home screen can export **any saved session**, not merely the most recent one, and can export all sessions together as a ZIP.

The application has no Internet permission. `android:allowBackup="false"` is set so session CSVs and calibration/settings are not included in ordinary Android Auto Backup by this app. Export occurs only through an explicit user action.

Android can still terminate an application process while it is backgrounded. Already checkpointed rows survive, but v0.1.2 does not yet implement full restoration/resumption of an in-progress Activity after process death.

## Calibration limitation

Calibration is checked against the phone's reported screen dimensions. Matching dimensions/resolution do **not** prove that an old calibration belongs to the same physical display. Recalibrate after changing phones/displays, display scaling, resolution, or presentation configuration.

## Scientific scope

This is a research prototype for binocular fusion, disparity, multistability, and observer-dependent 3-D interpretation. It is **not** a clinical diagnostic or treatment app, and it makes no claim that repeated use improves a medical vision condition.

## v0.1.2 validation

For this release, the pure-Java mathematical core was independently compiled with `javac`. All **289** entries emitted by the actual `StimulusCatalog.all()` implementation were compared with `fixtures/parent_manifest.csv`: **0 mismatches**. The actual Java trial planner was also stress-tested across **10,000 deterministic seeds** with **0 failures**, 40 trials per plan, eight hidden repeats, and minimum observed repeat lag of eight trials.

The mathematical catalog, stereo rendering equations, trial-planning algorithm, and parent fixture are byte-for-byte unchanged from v0.1.1. v0.1.2 changes validation, Android orientation/timing safeguards, CI enforcement, and release metadata only.
