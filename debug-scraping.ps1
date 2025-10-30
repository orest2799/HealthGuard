# Debug what content is actually on the Galinos page
$htmlFile = "galinos-test.html"

if (-not (Test-Path $htmlFile)) {
    Write-Host "[ERROR] File not found: $htmlFile" -ForegroundColor Red
    Write-Host "Run .\test-galinos-scraping.ps1 first" -ForegroundColor Yellow
    exit
}

Write-Host "Analyzing Galinos HTML..." -ForegroundColor Blue
Write-Host ""

$content = Get-Content $htmlFile -Raw

# Check 1: Look for h2 headers
Write-Host "Found H2 Headers:" -ForegroundColor Yellow
$h2Matches = [regex]::Matches($content, '<h2[^>]*>([^<]+)</h2>')
if ($h2Matches.Count -gt 0) {
    $h2Matches | ForEach-Object {
        Write-Host "  - $($_.Groups[1].Value)" -ForegroundColor Gray
    }
} else {
    Write-Host "  [NONE FOUND]" -ForegroundColor Red
}
Write-Host ""

# Check 2: Look for div.textile
Write-Host "Found div.textile sections:" -ForegroundColor Yellow
$textileMatches = [regex]::Matches($content, '<div class="textile">([^<]*<p>.*?</p>.*?)</div>', [System.Text.RegularExpressions.RegexOptions]::Singleline)
if ($textileMatches.Count -gt 0) {
    Write-Host "  Count: $($textileMatches.Count)" -ForegroundColor Gray
    $textileMatches | Select-Object -First 3 | ForEach-Object {
        $text = $_.Groups[1].Value -replace '<[^>]+>', '' -replace '\s+', ' '
        $preview = $text.Substring(0, [Math]::Min(100, $text.Length))
        Write-Host "  Preview: $preview..." -ForegroundColor Gray
    }
} else {
    Write-Host "  [NONE FOUND]" -ForegroundColor Red
}
Write-Host ""

# Check 3: Look for meta description
Write-Host "Meta Description:" -ForegroundColor Yellow
if ($content -match '<meta name="description" content="([^"]+)"') {
    Write-Host "  $($Matches[1])" -ForegroundColor Gray
} else {
    Write-Host "  [NOT FOUND]" -ForegroundColor Red
}
Write-Host ""

# Check 4: Look for main content article
Write-Host "Main Content Structure:" -ForegroundColor Yellow
if ($content -match '<article[^>]*id="main-content"') {
    Write-Host "  [OK] Found article#main-content" -ForegroundColor Green
} else {
    Write-Host "  [WARN] No article#main-content" -ForegroundColor Yellow
}
Write-Host ""

# Check 5: Extract a sample section manually
Write-Host "Sample Content Extraction:" -ForegroundColor Yellow
$sampleMatch = [regex]::Match($content, '<h2>(.*?)</h2>.*?<div class="textile">(.*?)</div>', [System.Text.RegularExpressions.RegexOptions]::Singleline)
if ($sampleMatch.Success) {
    $title = $sampleMatch.Groups[1].Value -replace '<[^>]+>', ''
    $text = $sampleMatch.Groups[2].Value -replace '<[^>]+>', '' -replace '\s+', ' '
    Write-Host "  Title: $title" -ForegroundColor Cyan
    Write-Host "  Content (first 200 chars): $($text.Substring(0, [Math]::Min(200, $text.Length)))..." -ForegroundColor Gray
} else {
    Write-Host "  [WARN] Could not extract sample section" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Based on the analysis above, we need to:" -ForegroundColor Gray
Write-Host "1. Update the scraper to match the actual HTML structure" -ForegroundColor White
Write-Host "2. Or add Galinos login credentials for full access" -ForegroundColor White
Write-Host ""
Write-Host "Open galinos-test.html in a browser to see the page visually" -ForegroundColor Cyan