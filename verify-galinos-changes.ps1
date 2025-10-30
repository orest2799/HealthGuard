# Verify GalinosProvider changes were applied
$galinosFile = "backend\src\main\kotlin\app\meds\GalinosProvider.kt"

Write-Host "Checking GalinosProvider.kt..." -ForegroundColor Yellow

if (Test-Path $galinosFile) {
    $content = Get-Content $galinosFile -Raw

    Write-Host "[CHECK 1] Checking for DuckDuckGo references..." -ForegroundColor Cyan
    if ($content -match "duckduckgo") {
        Write-Host "  [WARN] Still has DuckDuckGo code - update not applied!" -ForegroundColor Red
    } else {
        Write-Host "  [OK] No DuckDuckGo references found" -ForegroundColor Green
    }

    Write-Host "[CHECK 2] Checking for KNOWN_MEDICINES map..." -ForegroundColor Cyan
    if ($content -match "KNOWN_MEDICINES") {
        Write-Host "  [OK] Has KNOWN_MEDICINES - new code applied!" -ForegroundColor Green
    } else {
        Write-Host "  [WARN] Missing KNOWN_MEDICINES - old code still present!" -ForegroundColor Red
    }

    Write-Host "[CHECK 3] Checking for direct search method..." -ForegroundColor Cyan
    if ($content -match "findGalinosUrlDirect") {
        Write-Host "  [OK] Has findGalinosUrlDirect method" -ForegroundColor Green
    } else {
        Write-Host "  [WARN] Missing findGalinosUrlDirect method" -ForegroundColor Red
    }

} else {
    Write-Host "[ERROR] File not found: $galinosFile" -ForegroundColor Red
    Write-Host "Are you in the project root directory?" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "If any checks failed, you need to update the file!" -ForegroundColor Yellow