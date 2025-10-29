# =============================================================================
# HealthGuard Backend Setup and Run Script for Windows PowerShell
# =============================================================================

Write-Host "========================================" -ForegroundColor Blue
Write-Host "   HealthGuard Backend Setup" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# =============================================================================
# Step 1: Check Environment Variables
# =============================================================================
Write-Host "Step 1: Checking Environment Variables" -ForegroundColor Yellow
Write-Host "----------------------------------------"
Write-Host ""

if (-not $env:GEMINI_API_KEY) {
    Write-Host "[ERROR] GEMINI_API_KEY is not set" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please set your Gemini API key first:"
    Write-Host '  $env:GEMINI_API_KEY = "your-api-key-here"' -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Or add it permanently:"
    Write-Host '  [System.Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "your-key", "User")' -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Get your API key from: https://makersuite.google.com/app/apikey"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
} else {
    $keyStart = $env:GEMINI_API_KEY.Substring(0, [Math]::Min(10, $env:GEMINI_API_KEY.Length))
    $keyEnd = $env:GEMINI_API_KEY.Substring([Math]::Max(0, $env:GEMINI_API_KEY.Length - 4))
    Write-Host "[OK] GEMINI_API_KEY is set" -ForegroundColor Green
    Write-Host "     Key: $keyStart...$keyEnd"
}

Write-Host ""

if (-not $env:GCP_PROJECT_ID) {
    Write-Host "[WARN] GCP_PROJECT_ID not set (will use default: healthguard-b443f)" -ForegroundColor Yellow
} else {
    Write-Host "[OK] GCP_PROJECT_ID: $env:GCP_PROJECT_ID" -ForegroundColor Green
}

Write-Host ""
Write-Host "----------------------------------------"
Write-Host ""

# =============================================================================
# Step 2: Check Java/Gradle
# =============================================================================
Write-Host "Step 2: Checking Build Tools" -ForegroundColor Yellow
Write-Host "----------------------------------------"
Write-Host ""

$javaVersion = & java -version 2>&1 | Select-String "version"
if ($javaVersion) {
    Write-Host "[OK] Java found: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Java not found" -ForegroundColor Red
    Write-Host "Please install Java 11 or higher"
    Write-Host "Download from: https://adoptium.net/"
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not (Test-Path "gradlew.bat")) {
    Write-Host "[ERROR] gradlew.bat not found" -ForegroundColor Red
    Write-Host "Please run this script from the project root directory"
    Read-Host "Press Enter to exit"
    exit 1
} else {
    Write-Host "[OK] Gradle wrapper found" -ForegroundColor Green
}

Write-Host ""
Write-Host "----------------------------------------"
Write-Host ""

# =============================================================================
# Step 3: Clean and Build
# =============================================================================
Write-Host "Step 3: Building Project" -ForegroundColor Yellow
Write-Host "----------------------------------------"
Write-Host ""
Write-Host "Running: .\gradlew.bat clean build"
Write-Host ""

& .\gradlew.bat clean build

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Build failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common issues:"
    Write-Host "1. Missing dependencies - run: .\gradlew.bat --refresh-dependencies build"
    Write-Host "2. Kotlin version mismatch - check build.gradle.kts"
    Write-Host "3. Compilation errors - check the output above"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
} else {
    Write-Host ""
    Write-Host "[OK] Build successful!" -ForegroundColor Green
}

Write-Host ""
Write-Host "----------------------------------------"
Write-Host ""

# =============================================================================
# Step 4: Start Server
# =============================================================================
Write-Host "Step 4: Starting Server" -ForegroundColor Yellow
Write-Host "----------------------------------------"
Write-Host ""

if (-not $env:PORT) {
    $env:PORT = "8080"
}

Write-Host "Server will start on port: $env:PORT"
Write-Host ""
Write-Host "Endpoints available:"
Write-Host "  GET  http://localhost:$env:PORT/health"
Write-Host "  POST http://localhost:$env:PORT/chat"
Write-Host "  GET  http://localhost:$env:PORT/meds/search"
Write-Host "  POST http://localhost:$env:PORT/vision/annotate"
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Cyan
Write-Host ""
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# Run the server
& .\gradlew.bat run