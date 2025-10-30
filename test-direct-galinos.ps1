# =============================================================================
# Test Direct Galinos Access
# This tests if we can reach Galinos.gr directly
# =============================================================================

Write-Host "Testing Galinos.gr Access" -ForegroundColor Blue
Write-Host ""

# Test 1: Can we reach Galinos?
Write-Host "Test 1: Checking Galinos.gr..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "https://www.galinos.gr/" -UseBasicParsing -TimeoutSec 10
    Write-Host "[OK] Galinos.gr is accessible (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] Cannot reach Galinos.gr: $_" -ForegroundColor Red
}
Write-Host ""

# Test 2: Can we reach a specific drug page?
Write-Host "Test 2: Testing specific drug page..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "https://www.galinos.gr/web/drugs/main/substances/paracetamol" -UseBasicParsing -TimeoutSec 10
    Write-Host "[OK] Drug page accessible (Status: $($response.StatusCode))" -ForegroundColor Green
    Write-Host "Content length: $($response.Content.Length) chars" -ForegroundColor Gray
} catch {
    Write-Host "[FAIL] Cannot reach drug page: $_" -ForegroundColor Red
}
Write-Host ""

# Test 3: Can we reach DuckDuckGo?
Write-Host "Test 3: Testing DuckDuckGo..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "https://duckduckgo.com/" -UseBasicParsing -TimeoutSec 10
    Write-Host "[OK] DuckDuckGo is accessible (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] Cannot reach DuckDuckGo: $_" -ForegroundColor Red
}
Write-Host ""

# Test 4: Try DuckDuckGo search
Write-Host "Test 4: Testing DuckDuckGo search for Galinos..." -ForegroundColor Yellow
try {
    $searchUrl = "https://duckduckgo.com/html/?q=site:galinos.gr+Depon"
    $response = Invoke-WebRequest -Uri $searchUrl -UseBasicParsing -TimeoutSec 10
    Write-Host "[OK] Search completed (Status: $($response.StatusCode))" -ForegroundColor Green

    # Check if we got any galinos.gr links
    if ($response.Content -match "galinos\.gr") {
        Write-Host "[OK] Found Galinos links in results" -ForegroundColor Green
    } else {
        Write-Host "[WARN] No Galinos links found in results" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[FAIL] DuckDuckGo search failed: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Diagnosis:" -ForegroundColor Blue
Write-Host "If all tests pass, the issue might be with User-Agent or rate limiting." -ForegroundColor Gray
Write-Host "If DuckDuckGo fails, we need to use a different search approach." -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Blue