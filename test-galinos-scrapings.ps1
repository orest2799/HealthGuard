# Test what Galinos actually returns
Write-Host "Testing Galinos Scraping..." -ForegroundColor Blue
Write-Host ""

# Test 1: Direct URL access
Write-Host "Test 1: Accessing Galinos paracetamol page..." -ForegroundColor Yellow
$url = "https://www.galinos.gr/web/drugs/main/substances/paracetamol"

try {
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing

    Write-Host "[OK] Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Content length: $($response.Content.Length) chars" -ForegroundColor Gray

    # Check for login redirect
    if ($response.Content -match "account\?continue" -or $response.Content -match "login") {
        Write-Host "[WARN] Page requires login!" -ForegroundColor Yellow
    } else {
        Write-Host "[OK] No login required" -ForegroundColor Green
    }

    # Check for actual content (using English keywords to avoid encoding)
    if ($response.Content -match "indication" -or $response.Content -match "dosage" -or $response.Content -match "Paracetamol") {
        Write-Host "[OK] Found medicine information" -ForegroundColor Green
    } else {
        Write-Host "[WARN] No medicine information found" -ForegroundColor Yellow
    }

    # Save to file for inspection
    $response.Content | Out-File "galinos-test.html" -Encoding UTF8
    Write-Host ""
    Write-Host "Saved response to: galinos-test.html" -ForegroundColor Cyan

} catch {
    Write-Host "[FAIL] Error: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# Test 2: Check backend's medicine search
Write-Host "Test 2: Backend medicine search..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/meds/search?q=paracetamol&lang=el"

    if ($response.results.Count -gt 0) {
        Write-Host "[OK] Found $($response.results.Count) results" -ForegroundColor Green
        $response.results | Select-Object -First 3 | Format-Table brand, generic, source
    } else {
        Write-Host "[WARN] No results found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[FAIL] Error: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Blue
Write-Host "Diagnosis:" -ForegroundColor Yellow
Write-Host ""
Write-Host "If Test 1 shows 'requires login', Galinos blocks scrapers." -ForegroundColor Gray
Write-Host "If Test 2 shows 0 results, OpenFDA/EMA search isn't working." -ForegroundColor Gray
Write-Host ""
Write-Host "Solutions:" -ForegroundColor Cyan
Write-Host "1. Use OpenFDA for US medicines instead of Galinos" -ForegroundColor White
Write-Host "2. Add Galinos login credentials to the backend" -ForegroundColor White
Write-Host "3. Test with medicines that don't require login" -ForegroundColor White