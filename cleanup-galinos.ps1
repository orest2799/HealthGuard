# Check what needs to be removed from GalinosProvider.kt
$file = "backend\src\main\kotlin\app\meds\GalinosProvider.kt"

if (Test-Path $file) {
    $content = Get-Content $file -Raw

    Write-Host "Checking GalinosProvider.kt for old code..." -ForegroundColor Yellow
    Write-Host ""

    # Check for old DuckDuckGo method
    if ($content -match "findGalinosUrlWithDuckDuckGo") {
        Write-Host "[FOUND] Old method: findGalinosUrlWithDuckDuckGo" -ForegroundColor Red
        Write-Host "  This method should be removed!" -ForegroundColor Red
        Write-Host ""
    }

    # Check for old search logic
    if ($content -match "duckduckgo\.com") {
        Write-Host "[FOUND] DuckDuckGo URL references" -ForegroundColor Red
        Write-Host "  These should be removed!" -ForegroundColor Red
        Write-Host ""
    }

    Write-Host "SOLUTION: The file should be COMPLETELY REPLACED, not edited." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Steps to fix:" -ForegroundColor Cyan
    Write-Host "1. Open: backend\src\main\kotlin\app\meds\GalinosProvider.kt" -ForegroundColor White
    Write-Host "2. DELETE ALL CONTENT (Ctrl+A, Delete)" -ForegroundColor White
    Write-Host "3. PASTE the new code from the artifact" -ForegroundColor White
    Write-Host "4. Save the file" -ForegroundColor White
    Write-Host "5. Rebuild: .\gradlew.bat :backend:build" -ForegroundColor White

} else {
    Write-Host "[ERROR] File not found!" -ForegroundColor Red
}