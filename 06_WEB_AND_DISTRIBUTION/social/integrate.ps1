################################################################################
# integrate.ps1
# ─────────────
# Script لدمج ملفات social/ في تطبيق MadarFlutter
# شغّله بس لما تكون جاهز للدمج الحقيقي
# 
# تشغيل:
#   cd "<project_root>"
#   .\social\integrate.ps1
################################################################################

$root    = $PSScriptRoot | Split-Path
$social  = Join-Path $root "social"
$flutter = Join-Path $root "MadarFlutter"
$android = Join-Path $flutter "android\app\src\main\kotlin\com\madar\tv\social"

Write-Host "=== زقزوقة - Integration Script ===" -ForegroundColor Cyan

# 1. تكوين مجلد الـ Kotlin
Write-Host "`n[1] Creating Kotlin package directory..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path $android | Out-Null
Write-Host "    ✓ $android" -ForegroundColor Green

# 2. نسخ ملفات Kotlin
Write-Host "`n[2] Copying Kotlin files..." -ForegroundColor Yellow
$ktFiles = @(
    "QualityTrackExtractor.kt",
    "SmartMediaLinker.kt",
    "RamadanEngine.kt",
    "AutoCrawlerWorker.kt",
    "SuperProviderBridge.kt"
)
foreach ($f in $ktFiles) {
    $src = Join-Path $social "kotlin\$f"
    $dst = Join-Path $android $f
    Copy-Item $src $dst -Force
    Write-Host "    ✓ $f" -ForegroundColor Green
}

# 3. نسخ ملفات Dart
Write-Host "`n[3] Copying Dart files..." -ForegroundColor Yellow
$dartSrc = Join-Path $social "dart"
$dartDst = Join-Path $flutter "lib\src"
$dartFiles = @(
    "quality_selector.dart",
    "server_merger.dart",
    "ramadan_section.dart",
    "smart_linker_client.dart"
)
foreach ($f in $dartFiles) {
    $src = Join-Path $dartSrc $f
    $dst = Join-Path $dartDst $f
    Copy-Item $src $dst -Force
    Write-Host "    ✓ $f" -ForegroundColor Green
}

# 4. تحقق
Write-Host "`n[4] Verification..." -ForegroundColor Yellow
$allOk = $true
foreach ($f in $ktFiles) {
    if (!(Test-Path (Join-Path $android $f))) {
        Write-Host "    ✗ MISSING: $f" -ForegroundColor Red
        $allOk = $false
    }
}
foreach ($f in $dartFiles) {
    if (!(Test-Path (Join-Path $dartDst $f))) {
        Write-Host "    ✗ MISSING: $f" -ForegroundColor Red
        $allOk = $false
    }
}

if ($allOk) {
    Write-Host "`n✅ All files copied successfully!" -ForegroundColor Green
    Write-Host "`nNext steps:" -ForegroundColor Cyan
    Write-Host "  1. Add to MadarFlutter/android/app/build.gradle:" -ForegroundColor White
    Write-Host "       implementation 'androidx.work:work-runtime-ktx:2.9.0'" -ForegroundColor Gray
    Write-Host "  2. Add to MadarFlutter/pubspec.yaml:" -ForegroundColor White
    Write-Host "       shared_preferences: ^2.2.0" -ForegroundColor Gray
    Write-Host "  3. Run: flutter pub get" -ForegroundColor White
    Write-Host "  4. Run: flutter build apk --release" -ForegroundColor White
} else {
    Write-Host "`n❌ Some files are missing. Check the errors above." -ForegroundColor Red
}
