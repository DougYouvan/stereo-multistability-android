$ErrorActionPreference = "Stop"
$alias = "stereo-multistability"
$keystore = "stereo-multistability-release.jks"
Write-Host "This creates your permanent Android signing key. BACK IT UP. Do not commit it to GitHub."
keytool -genkeypair -v -keystore $keystore -alias $alias -keyalg RSA -keysize 4096 -validity 10000
$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $keystore))
[Convert]::ToBase64String($bytes) | Set-Content -NoNewline "android-keystore-base64.txt"
Write-Host "Created: $keystore"
Write-Host "Created: android-keystore-base64.txt"
Write-Host "Add the base64 file contents to GitHub secret ANDROID_KEYSTORE_BASE64."
Write-Host "Also add ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS=$alias, and ANDROID_KEY_PASSWORD."
