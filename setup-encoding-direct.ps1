# Прямое выполнение команд настройки кодировки (без сохранения в файл)
# Скопируйте и вставьте эти команды прямо в PowerShell

Write-Host "Настройка кодировки PowerShell для корректного отображения кириллицы..." -ForegroundColor Green

# Настройка для текущей сессии PowerShell
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
chcp 65001 | Out-Null

# Проверка и создание профиля PowerShell
$profilePath = $PROFILE.CurrentUserAllHosts
$profileDir = Split-Path -Path $profilePath -Parent

if (-not (Test-Path $profileDir)) {
    New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
    Write-Host "Создана директория профиля: $profileDir" -ForegroundColor Green
}

# Добавление настроек в профиль
$profileContent = @"
# Настройка кодировки UTF-8 для PowerShell
`$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
`$PSDefaultParameterValues['*:Encoding'] = 'utf8'

# Установка кодовой страницы консоли
chcp 65001 | Out-Null
"@

if (Test-Path $profilePath) {
    $existingContent = Get-Content $profilePath -Raw -ErrorAction SilentlyContinue
    if ($existingContent -notlike "*OutputEncoding*") {
        Add-Content -Path $profilePath -Value "`n$profileContent"
        Write-Host "Настройки добавлены в профиль: $profilePath" -ForegroundColor Green
    } else {
        Write-Host "Профиль уже содержит настройки кодировки." -ForegroundColor Yellow
    }
} else {
    Set-Content -Path $profilePath -Value $profileContent
    Write-Host "Создан профиль PowerShell с настройками кодировки: $profilePath" -ForegroundColor Green
}

Write-Host "`nНастройка завершена!" -ForegroundColor Green
Write-Host "Проверка: попробуйте вывести русский текст:" -ForegroundColor Cyan
Write-Host "  Write-Host 'Привет, мир! Тест кириллицы: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ'" -ForegroundColor Gray
Write-Host "`nДля применения настроек в новых сессиях PowerShell перезапустите терминал." -ForegroundColor Yellow

