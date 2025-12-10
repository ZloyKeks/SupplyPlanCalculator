# PowerShell script for UTF-8 encoding setup
# Run as administrator for global setup, or without admin rights for current user only

Write-Host "Setting up PowerShell encoding for correct Cyrillic display..." -ForegroundColor Green

# Check for administrator rights
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if ($isAdmin) {
    Write-Host "Administrator rights detected. Setting up global encoding..." -ForegroundColor Yellow
    
    # Set UTF-8 encoding for Windows console (globally)
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    [Console]::InputEncoding = [System.Text.Encoding]::UTF8
    
    # Set encoding for PowerShell via registry
    $codePage = 65001  # UTF-8
    Set-ItemProperty -Path "HKCU:\Console" -Name "CodePage" -Value $codePage -ErrorAction SilentlyContinue
    
    Write-Host "Global settings applied." -ForegroundColor Green
} else {
    Write-Host "Running without administrator rights. Setting up for current session only..." -ForegroundColor Yellow
}

# Setup for current PowerShell session
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

# Set encoding for file operations
$PSDefaultParameterValues['*:Encoding'] = 'utf8'

# Check and create PowerShell profile
$profilePath = $PROFILE.CurrentUserAllHosts
$profileDir = Split-Path -Path $profilePath -Parent

if (-not (Test-Path $profileDir)) {
    New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
    Write-Host "Profile directory created: $profileDir" -ForegroundColor Green
}

# Add settings to profile
$profileContent = @"
# UTF-8 encoding setup for PowerShell
`$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
`$PSDefaultParameterValues['*:Encoding'] = 'utf8'

# Set console code page
chcp 65001 | Out-Null
"@

if (Test-Path $profilePath) {
    $existingContent = Get-Content $profilePath -Raw -ErrorAction SilentlyContinue
    $searchPattern = "OutputEncoding"
    if ($existingContent -notmatch $searchPattern) {
        Add-Content -Path $profilePath -Value "`n$profileContent"
        Write-Host "Settings added to profile: $profilePath" -ForegroundColor Green
    } else {
        Write-Host "Profile already contains encoding settings." -ForegroundColor Yellow
    }
} else {
    Set-Content -Path $profilePath -Value $profileContent -Encoding UTF8
    Write-Host "PowerShell profile created with encoding settings: $profilePath" -ForegroundColor Green
}

# Apply settings for current session
chcp 65001 | Out-Null

Write-Host ""
Write-Host "Setup completed!" -ForegroundColor Green
Write-Host "Test: try to output Russian text:" -ForegroundColor Cyan
Write-Host "  Write-Host 'Privet, mir! Test kirillitsy: ABVGDEYOZHZIJKLMNOPRSTUFKHTSCHSHSHCHYEYUYA'" -ForegroundColor Gray
Write-Host ""
Write-Host "Restart PowerShell terminal to apply settings in new sessions." -ForegroundColor Yellow
