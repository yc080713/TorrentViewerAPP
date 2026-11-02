@echo off
setlocal
cd /d "%~dp0"

rem Build debug APK. Requires JDK 17-21 (JDK 22+ is not supported by Gradle 8.13).
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [!] JAVA_HOME is not set or invalid. Please install JDK 17-21 and set JAVA_HOME.
)

echo ============================================
echo   Building TorrentViewer (debug APK)
echo ============================================
echo.

call gradlew.bat assembleDebug
if errorlevel 1 (
    echo.
    echo [FAILED] Build failed. See the log above.
    exit /b 1
)

echo.
echo [OK] APK generated:
echo   %~dp0app\build\outputs\apk\debug\app-debug.apk
endlocal
