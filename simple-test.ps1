
# =============================================================================
# Simple Backend Test Script (English only to avoid encoding issues)
# =============================================================================

$BASE_URL = "http://localhost:8080"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Testing HealthGuard Backend" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# =============================================================================
# Test 1: Health Check
# =============================================================================
Write-Host "Test 1: Health Check" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/health"
    Write-Host "[OK] Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response: $($response.Content)" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] $_" -ForegroundColor Red
}
Write-Host ""

# =============================================================================
# Test 2: Simple Chat (English)
# =============================================================================
Write-Host "Test 2: Simple Chat (English)" -ForegroundColor Yellow
$body = @{
    text = "Hello"
} | ConvertTo-Json

Write-Host "Sending: $body" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/chat" -Method POST -Body $body -ContentType "application/json"
    Write-Host "[OK] Got response" -ForegroundColor Green
    Write-Host "SessionId: $($response.sessionId)" -ForegroundColor Cyan
    Write-Host "Reply: $($response.reply)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Full JSON:" -ForegroundColor Gray
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "[FAIL] Error: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Details: $errorBody" -ForegroundColor Red
    }
}
Write-Host ""

# =============================================================================
# Test 3: Chat with Medicine (English medicine name)
# =============================================================================
Write-Host "Test 3: Chat with Medicine Query" -ForegroundColor Yellow
$body = @{
    text = "What are the indications?"
    medQuery = "Depon"
} | ConvertTo-Json

Write-Host "Sending: $body" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/chat" -Method POST -Body $body -ContentType "application/json"
    Write-Host "[OK] Got response" -ForegroundColor Green
    Write-Host "Reply: $($response.reply)" -ForegroundColor Cyan
    if ($response.metadata.sources) {
        Write-Host "Sources:" -ForegroundColor Cyan
        $response.metadata.sources | ForEach-Object {
            Write-Host "  - $($_.title): $($_.url)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "[FAIL] Error: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Details: $errorBody" -ForegroundColor Red
    }
}
Write-Host ""

# =============================================================================
# Test 4: Medicine Search
# =============================================================================
Write-Host "Test 4: Medicine Search" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/meds/search?q=Depon&lang=el"
    Write-Host "[OK] Got response" -ForegroundColor Green
    Write-Host "Found $($response.results.Count) results" -ForegroundColor Cyan
    $response.results | Select-Object -First 3 | ForEach-Object {
        Write-Host "  - $($_.brand) ($($_.generic))" -ForegroundColor Gray
    }
} catch {
    Write-Host "[FAIL] Error: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Tests Complete" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue