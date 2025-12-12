# Script to permanently set JAVA_HOME to JDK 25
# Run this script as Administrator

Write-Host "Setting JAVA_HOME to JDK 25..." -ForegroundColor Green

# Set JAVA_HOME for the current user
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-25", "User")

Write-Host "JAVA_HOME has been set to: C:\Program Files\Java\jdk-25" -ForegroundColor Green
Write-Host ""
Write-Host "Please restart your terminal/IDE for the changes to take effect." -ForegroundColor Yellow
Write-Host ""
Write-Host "To verify, run:" -ForegroundColor Cyan
Write-Host "  `$env:JAVA_HOME" -ForegroundColor White
Write-Host "  mvn -version" -ForegroundColor White


