#!/usr/bin/env python3
"""Static release-integrity checks that do not require an Android SDK."""
from pathlib import Path
import re, csv
ROOT=Path(__file__).resolve().parents[1]
main=(ROOT/'app/src/main/java/ai/youvan/stereomultistability/MainActivity.java').read_text()
store=(ROOT/'app/src/main/java/ai/youvan/stereomultistability/SessionStore.java').read_text()
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
build=(ROOT/'app/build.gradle').read_text()
release=(ROOT/'.github/workflows/release.yml').read_text()
ci=(ROOT/'.github/workflows/ci.yml').read_text()
fixture=ROOT/'fixtures/parent_manifest.csv'

assert 'targetSdk 36' in build
assert "versionName '0.1.2'" in build and 'versionCode 3' in build
assert 'android:allowBackup="false"' in manifest
assert 'uses-permission' not in manifest
assert 'Stereo Multistability Android' in manifest
assert 'OnBackInvokedDispatcher.PRIORITY_DEFAULT' in main
assert 'registerOnBackInvokedCallback' in main
assert 'showFreeReport' in main and 'showStructuredResponse' in main
# The free-report screen must be created without structured category strings.
free=main.split('private void showFreeReport',1)[1].split('private void showStructuredResponse',1)[0]
for category in ('Top stack','Front stack','Depth orientation','Divider','Kink / notch'):
    assert category not in free, category
assert 'Enter a short free report before continuing.' in free
assert 'Export a saved session CSV' in main and 'Export all saved sessions as ZIP' in main
assert 'createAllSessionsZip' in store and 'listSessions' in store
assert 'FLAG_KEEP_SCREEN_ON' in main
assert 'WindowInsetsController' in main
assert 'PARENT_MANIFEST_SHA256' in main
assert 'record_type,oscillation_session_id,parent_manifest_sha256,session_status' in main
assert 'record_type,session_id,parent_manifest_sha256,session_status,planned_trials,completed_trials' in main
assert '"completed"' in main and '"aborted"' in main
assert 'Verify requested tag matches Android versionName' in release
assert 'python tools/validate_release.py' in ci
assert 'python tools/validate_release.py' in release
assert "resources.srcDir rootProject.file('fixtures')" in build
assert 'actualJavaCatalogMatchesAll289ParentFixtureRows' in (ROOT/'app/src/test/java/ai/youvan/stereomultistability/StimulusCatalogTest.java').read_text()
assert 'Landscape required' in main and 'isLandscapeNow' in main
assert 'stimulusClockReady' in main and 'oscillationClockReady' in main
with fixture.open(newline='',encoding='utf-8') as f:
    rows=list(csv.DictReader(f))
assert len(rows)==289 and rows[0]['stimulus_id']=='S0001' and rows[-1]['stimulus_id']=='S0289'
print('Release-integrity static checks OK; parent fixture rows:',len(rows))
