# =============================================================================
# Test HealthGuard Backend - PowerShell Version
# Run this in a NEW PowerShell window while the server is running
# =============================================================================

$BASE_URL = "http://localhost:8080"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "   Testing HealthGuard Backend" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# =============================================================================
# Test 1: Health Check
# =============================================================================
Write-Host "Test 1: Health Check" -ForegroundColor Yellow
Write-Host "GET $BASE_URL/health" -ForegroundColor Gray
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/health" -Method GET
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response: $($response.Content)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "----------------------------------------"
Write-Host ""

# =============================================================================
# Test 2: Simple Chat
# =============================================================================
Write-Host "Test 2: Simple Chat" -ForegroundColor Yellow
Write-Host "POST $BASE_URL/chat" -ForegroundColor Gray
Write-Host ""

$body = @{
    text = "Γεια σου"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/chat" -Method POST -Body $body -ContentType "application/json"
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    Write-Host "Details: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "----------------------------------------"
Write-Host ""

# =============================================================================
# Test 3: Chat with Medicine Query (Depon)
# =============================================================================
Write-Host "Test 3: Chat with Medicine Query (Depon)" -ForegroundColor Yellow
Write-Host "POST $BASE_URL/chat" -ForegroundColor Gray
Write-Host ""

$body = @{
    text = "Ποιες είναι οι ενδείξεις;"
    medQuery = "Depon"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/chat" -Method POST -Body $body -ContentType "application/json"
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host

    Write-Host ""
    Write-Host "Reply:" -ForegroundColor Cyan
    Write-Host $response.reply -ForegroundColor White
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    Write-Host "Details: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "----------------------------------------"
Write-Host ""

# =============================================================================
# Test 4: Chat with OCR Context
# =============================================================================
Write-Host "Test 4: Chat with OCR Context" -ForegroundColor Yellow
Write-Host "POST $BASE_URL/chat" -ForegroundColor Gray
Write-Host ""

$body = @{
    text = "Τι φάρμακο είναι αυτό;"
    context = @{
        ocrText = "Augmentin 500mg"
    }
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/chat" -Method POST -Body $body -ContentType "application/json"
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host

    Write-Host ""
    Write-Host "Reply:" -ForegroundColor Cyan
    Write-Host $response.reply -ForegroundColor White
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    Write-Host "Details: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "----------------------------------------"
Write-Host ""

# =============================================================================
# Test 5: Medicine Search
# =============================================================================
Write-Host "Test 5: Medicine Search" -ForegroundColor Yellow
Write-Host "GET $BASE_URL/meds/search?q=Depon&lang=el" -ForegroundColor Gray
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/meds/search?q=Depon&lang=el" -Method GET
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host

    if ($response.results) {
        Write-Host ""
        Write-Host "Found $($response.results.Count) results" -ForegroundColor Cyan
    }
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    Write-Host "Details: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Blue
Write-Host "   Tests Complete!" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""