# test-greek.ps1
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 > $null

Write-Host "Testing Greek Response..." -ForegroundColor Blue

$body = @{
    text = "Ti einai afto? Apantise sta ellinika."
    medQuery = "paracetamol"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/chat" -Method POST -Body $body -ContentType "application/json"

Write-Host "`nGreek Reply:" -ForegroundColor Green
Write-Host $response.reply