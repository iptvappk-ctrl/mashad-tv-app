@echo off
title Mashad IPTV Ecosystem - Control Center
chcp 65001 >nul
color 0E

:menu
cls
echo ================================================================
echo       📺 منظومة مَـشْـهَـدْ TV — مركز التحكم والتشغيل
echo ================================================================
echo.
echo  [1] تشغيل خادم التحليلات والرصد (Port 4040) وفتح اللوحة
echo  [2] تشغيل منصة الويب السينمائية (Port 3000)
echo  [3] بناء تطبيق الأندرويد (Release APK مع R8)
echo  [4] فتح لوحة التحكم في المتصفح مباشرة
echo  [5] تشغيل كل الخدمات معاً (السيرفر + منصة الويب + اللوحة)
echo  [0] خروج
echo.
echo ================================================================
set /p opt="اختر رقم العملية (0-5): "

if "%opt%"=="1" goto start_dashboard
if "%opt%"=="2" goto start_web
if "%opt%"=="3" goto build_apk
if "%opt%"=="4" goto open_dash
if "%opt%"=="5" goto start_all
if "%opt%"=="0" exit
goto menu

:start_dashboard
cls
echo [!] جاري تشغيل لوحة تحليلات GA4...
netstat -ano | findstr ":4040" >nul
if %errorlevel% neq 0 (
    start "Mashad Analytics Server" /min /D "%~dp0dashboard" cmd /c npm start
    timeout /t 2 /nobreak >nul
)
start "" "http://127.0.0.1:4040"
echo [✓] اللوحة تعمل الآن على: http://127.0.0.1:4040
pause
goto menu

:start_web
cls
echo [!] جاري تشغيل منصة الويب (Next.js)...
cd /d "%~dp0web"
start "Mashad Web App" cmd /k "npm run dev"
timeout /t 3 /nobreak >nul
start "" "http://localhost:3000"
echo [✓] منصة الويب تعمل الآن على: http://localhost:3000
pause
goto menu

:build_apk
cls
echo [!] جاري بناء تطبيق الأندرويد (Release APK)...
cd /d "%~dp0MadarTV"
call gradlew.bat assembleRelease
if %errorlevel% equ 0 (
    if exist "app\build\outputs\apk\release\app-release.apk" (
        copy /y "app\build\outputs\apk\release\app-release.apk" "%USERPROFILE%\Desktop\Mashad.apk" >nul
    ) else (
        copy /y "app\build\outputs\apk\release\app-release-unsigned.apk" "%USERPROFILE%\Desktop\Mashad.apk" >nul
    )
    echo [✓] تم البناء ونسخ الملف إلى سطح المكتب باسم Mashad.apk. قد يكون غير موقّع.
) else (
    echo [X] فشل البناء! يرجى مراجعة سجل الأخطاء أعلاه.
)
pause
goto menu

:open_dash
start "" "http://127.0.0.1:4040"
goto menu

:start_all
cls
echo [!] جاري تشغيل كافة الخدمات...
netstat -ano | findstr ":4040" >nul
if %errorlevel% neq 0 (
    start "Mashad Analytics Server" /min /D "%~dp0dashboard" cmd /c npm start
    timeout /t 2 /nobreak >nul
)
cd /d "%~dp0web"
start "Mashad Web App" /min cmd /c "npm run dev"
timeout /t 3 /nobreak >nul
start "" "http://localhost:4040"
start "" "http://localhost:3000"
echo [✓] تم تشغيل الخادم والويب ولوحة التحكم بنجاح!
pause
goto menu
