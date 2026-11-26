[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [Parameter(Mandatory = $true)]
    [string]$VersionName,

    [Parameter(Mandatory = $true)]
    [long]$VersionCode,

    [Parameter(Mandatory = $true)]
    [string]$ReleaseNotes,

    [string]$Repository = "iptvappk-ctrl/madartv-updates",

    [string]$RepositoryPath = (Join-Path $env:LOCALAPPDATA "MadarTV\public-updates")
)

$ErrorActionPreference = "Stop"
$appPackage = "com.madar.madar_flutter"
$apk = (Resolve-Path -LiteralPath $ApkPath).Path
if ([IO.Path]::GetExtension($apk) -ne ".apk") { throw "ApkPath must point to an APK file." }
if ($VersionCode -le 0 -or $VersionName -notmatch '^[0-9]+\.[0-9]+\.[0-9]+$') {
    throw "Use the Android versionName and an increasing positive versionCode."
}

gh auth status | Out-Null

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$androidDir = Join-Path $projectRoot "android"
$sdkRoot = $env:ANDROID_SDK_ROOT
if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_HOME }
if (-not $sdkRoot) {
    $sdkLine = Get-Content (Join-Path $androidDir "local.properties") |
        Where-Object { $_ -match '^sdk\.dir=' } |
        Select-Object -First 1
    if ($sdkLine) {
        $sdkRoot = $sdkLine.Substring(8) -replace '\\\\', '\' -replace '\\:', ':'
    }
}
if (-not $sdkRoot -or -not (Test-Path -LiteralPath $sdkRoot)) {
    throw "Android SDK path was not found; set ANDROID_SDK_ROOT and retry."
}

$buildTools = Get-ChildItem -LiteralPath (Join-Path $sdkRoot "build-tools") -Directory |
    Sort-Object { [version]$_.Name } -Descending |
    Select-Object -First 1
$aapt = Join-Path $buildTools.FullName "aapt.exe"
$apksigner = Join-Path $buildTools.FullName "apksigner.bat"
if (-not (Test-Path -LiteralPath $aapt) -or -not (Test-Path -LiteralPath $apksigner)) {
    throw "Android build tools aapt/apksigner were not found."
}

$badging = (& $aapt dump badging $apk)[0]
if ($LASTEXITCODE -ne 0 -or $badging -notmatch "package: name='(?<package>[^']+)' versionCode='(?<code>[^']+)' versionName='(?<version>[^']+)'") {
    throw "Unable to read APK package/version metadata."
}
if ($Matches.package -ne $appPackage -or [long]$Matches.code -ne $VersionCode -or $Matches.version -ne $VersionName) {
    throw "APK metadata does not match the supplied package/version values."
}
& $apksigner verify --verbose $apk | Out-Null
if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed." }

$tag = "v$VersionName-$VersionCode"
$assetName = "zaqzouka-$VersionName-$VersionCode.apk"
$assetPath = Join-Path ([IO.Path]::GetTempPath()) $assetName
Copy-Item -LiteralPath $apk -Destination $assetPath -Force
$sha256 = (Get-FileHash -LiteralPath $assetPath -Algorithm SHA256).Hash.ToLowerInvariant()

$releaseExists = $false
try {
    gh release view $tag --repo $Repository --json tagName | Out-Null
    $releaseExists = $LASTEXITCODE -eq 0
} catch { $releaseExists = $false }

$notesFile = Join-Path ([IO.Path]::GetTempPath()) "madartv-release-notes-$VersionCode.txt"
Set-Content -LiteralPath $notesFile -Value $ReleaseNotes -Encoding utf8
if ($releaseExists) {
    gh release upload $tag $assetPath --repo $Repository --clobber
} else {
    gh release create $tag $assetPath --repo $Repository --title "زقزوقة $VersionName ($VersionCode)" --notes-file $notesFile
}
if ($LASTEXITCODE -ne 0) { throw "GitHub Release upload failed." }

# Also upload the companion ZAG Player APK if available
$playerApk = "<project_root>\realses\zag-player.apk"
$hasPlayer = Test-Path -LiteralPath $playerApk
$playerObj = $null
if ($hasPlayer) {
    $playerBadging = (& $aapt dump badging $playerApk)[0]
    $playerVersionCode = 27
    $playerVersionName = "1.1.27"
    if ($playerBadging -match "package: name='(?<package>[^']+)' versionCode='(?<code>[^']+)' versionName='(?<version>[^']+)'") {
        $playerVersionCode = [long]$Matches.code
        $playerVersionName = $Matches.version
    }
    $playerSha = (Get-FileHash -LiteralPath $playerApk -Algorithm SHA256).Hash.ToLowerInvariant()
    $playerAssetName = "zag-player-$playerVersionName.apk"
    $playerTempPath = Join-Path ([IO.Path]::GetTempPath()) $playerAssetName
    Copy-Item -LiteralPath $playerApk -Destination $playerTempPath -Force
    gh release upload $tag $playerTempPath --repo $Repository --clobber
    $playerObj = [ordered]@{
        packageName = "com.mader.player"
        versionCode = $playerVersionCode
        versionName = $playerVersionName
        apkUrl = "https://github.com/$Repository/releases/download/$tag/$playerAssetName"
        sha256 = $playerSha
        releaseNotes = "ZAG Player companion player update"
    }
}

if (-not (Test-Path -LiteralPath $RepositoryPath)) {
    $parent = Split-Path -Parent $RepositoryPath
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    gh repo clone $Repository $RepositoryPath
}
$remote = (git -C $RepositoryPath remote get-url origin).Trim()
if ($remote -notmatch ([regex]::Escape($Repository))) { throw "RepositoryPath points to a different Git remote." }
git -C $RepositoryPath pull --rebase origin main
if ($LASTEXITCODE -ne 0) { throw "Could not update the public feed checkout." }

$manifest = [ordered]@{
    packageId = $appPackage
    versionName = $VersionName
    versionCode = $VersionCode
    apkUrl = "https://github.com/$Repository/releases/download/$tag/$assetName"
    sha256 = $sha256
    releaseNotes = $ReleaseNotes
}
if ($playerObj) { $manifest["player"] = $playerObj }
$manifestPath = Join-Path $RepositoryPath "latest.json"
$manifestJson = $manifest | ConvertTo-Json -Depth 4
[IO.File]::WriteAllText($manifestPath, $manifestJson, [Text.UTF8Encoding]::new($false))
git -C $RepositoryPath add -- latest.json
git -C $RepositoryPath diff --cached --quiet -- latest.json
if ($LASTEXITCODE -eq 1) {
    git -C $RepositoryPath commit -m "Publish Android $VersionName ($VersionCode)"
    if ($LASTEXITCODE -ne 0) { throw "Could not commit latest.json." }
    git -C $RepositoryPath push origin main
    if ($LASTEXITCODE -ne 0) { throw "Could not publish latest.json to the public feed." }
} elseif ($LASTEXITCODE -ne 0) {
    throw "Could not check the public manifest changes."
}

# Firebase Hosting on the free Spark plan serves only the small update manifest.
# APK binaries stay on the public GitHub Release because Spark blocks .apk files.
$hostingDirectory = Join-Path $PSScriptRoot "..\firebase-update-hosting"
$hostingManifestPath = Join-Path $hostingDirectory "public\updates\android\latest.json"
if (-not (Test-Path -LiteralPath (Join-Path $hostingDirectory "firebase.json"))) {
    throw "Firebase Hosting configuration was not found at $hostingDirectory."
}
$firebaseManifest = [ordered]@{
    packageName = $appPackage
    versionCode = $VersionCode
    versionName = $VersionName
    apkUrl = "https://github.com/$Repository/releases/download/$tag/$assetName"
    sha256 = $sha256
    mandatory = $false
    releaseNotes = $ReleaseNotes
}
if ($playerObj) { $firebaseManifest["player"] = $playerObj }
$firebaseJson = $firebaseManifest | ConvertTo-Json -Depth 4
[IO.File]::WriteAllText($hostingManifestPath, $firebaseJson, [Text.UTF8Encoding]::new($false))
Push-Location $hostingDirectory
try {
    firebase deploy --only hosting --project madartv --non-interactive
    if ($LASTEXITCODE -ne 0) { throw "Firebase update manifest deployment failed." }
} finally {
    Pop-Location
}

[pscustomobject]@{
    Repository = "https://github.com/$Repository"
    Release = "https://github.com/$Repository/releases/tag/$tag"
    Version = "$VersionName+$VersionCode"
    ApkSha256 = $sha256
    Manifest = "https://raw.githubusercontent.com/$Repository/main/latest.json"
    FirebaseManifest = "https://madartv-updates.web.app/updates/android/latest.json"
}
