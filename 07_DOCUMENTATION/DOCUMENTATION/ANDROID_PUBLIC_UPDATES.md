# Android updates through Firebase Hosting

The app checks the public Firebase Hosting manifest at:

`https://madartv-updates.web.app/updates/android/latest.json`

Firebase Hosting is the source of update version, release notes, APK URL, and SHA-256. The APK itself is hosted as an asset in the public `iptvappk-ctrl/madartv-updates` GitHub Release. This split works with Firebase's free Spark plan because Spark blocks `.apk` files on Hosting. The manifest is ordinary JSON and does not require Firebase Authentication, invited testers, a paid plan, or a Play Store release.

## Update flow

1. The Android app checks the Firebase manifest after its first screen is shown.
2. If the manifest version code is newer, the app shows «يوجد تحديث جديد» and «تحديث الآن».
3. The app downloads the APK from the matching public GitHub release asset and reports progress.
4. Before opening Android's installer, the app checks the package name, version code, SHA-256, and signing certificate.
5. Android asks the user to allow installs from this app if needed, then shows its standard install confirmation. A sideloaded app cannot silently install updates.

If Firebase Hosting is unreachable or returns invalid metadata, the app falls back to GitHub's public latest-release API. This allows older installations to migrate and leaves a secondary feed available.

## Publish an update

The release publisher validates APK package/version/signature, uploads the APK to the public GitHub Release, updates the compatibility feed for older builds, and deploys the small Firebase manifest:

```powershell
Set-Location .\MadarFlutter
flutter build apk --release --no-pub
.\scripts\publish_public_update.ps1 `
  -ApkPath .\build\app\outputs\flutter-apk\app-release.apk `
  -VersionName 1.0.50 `
  -VersionCode 55 `
  -ReleaseNotes 'تحسينات وإصلاحات'
```

The APK is released first; Firebase Hosting is updated afterward with its URL and digest so the manifest never points to an asset that has not yet been uploaded. The hosting configuration and JSON are in `MadarFlutter/firebase-update-hosting`.

The Firebase manifest format is:

```json
{
  "packageName": "com.madar.madar_flutter",
  "versionCode": 55,
  "versionName": "1.0.50",
  "apkUrl": "https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.50-55/zaqzouka-1.0.50-55.apk",
  "sha256": "64 lowercase hexadecimal characters",
  "mandatory": false,
  "releaseNotes": "تحسينات وإصلاحات"
}
```

## Free-plan boundary

Firebase Spark remains sufficient for the JSON manifest. Its free Hosting limits still apply to manifest traffic. APK downloads use the public GitHub Release, avoiding both Firebase's Spark APK restriction and Firebase Storage billing. Android's user-approved installer remains part of the update flow.
