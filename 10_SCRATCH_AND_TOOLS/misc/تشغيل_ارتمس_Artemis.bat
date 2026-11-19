@echo off
title Artemis Mobile Agent
cd /d "C:\Users\hassa\Desktop\all project\artemis"

echo ======================================================
echo           Starting Artemis Mobile Agent
echo ======================================================
echo.

rem 1. Start Emulator and Phone Screen (scrcpy)
call "start-emulator.bat"

rem 2. Start Artemis Server & Web UI
echo [INFO] Starting Artemis Web UI Server...
start "Artemis Server" "%USERPROFILE%\.local\bin\uv.exe" run python -m artemis ui --port 8000 --open
