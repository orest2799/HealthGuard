@echo off
REM =============================================================================
REM Run HealthGuard Backend Server
REM =============================================================================

echo ========================================
echo   Starting HealthGuard Backend
echo ========================================
echo.

REM Check if API key is set
if "%GEMINI_API_KEY%"=="" (
    echo [ERROR] GEMINI_API_KEY is not set
    echo.
    echo Set it first:
    echo   set GEMINI_API_KEY=your-api-key-here
    echo.
    pause
    exit /b 1
)

REM Show masked key
set "KEY_START=%GEMINI_API_KEY:~0,10%"
set "KEY_END=%GEMINI_API_KEY:~-4%"
echo [OK] Using API Key: %KEY_START%...%KEY_END%
echo.

REM Set port if not defined
if "%PORT%"=="" set PORT=8080

echo Starting server on port %PORT%...
echo.
echo Available endpoints:
echo   GET  http://localhost:%PORT%/health
echo   POST http://localhost:%PORT%/chat
echo   GET  http://localhost:%PORT%/meds/search
echo   POST http://localhost:%PORT%/vision/annotate
echo.
echo Press Ctrl+C to stop the server
echo ========================================
echo.

REM Navigate to backend directory
cd /d "%~dp0backend"

REM Run the backend server
..\gradlew.bat :backend:run