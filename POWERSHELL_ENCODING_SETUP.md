# Настройка кодировки PowerShell для корректного отображения кириллицы

## Проблема
При работе в PowerShell на Windows могут отображаться кракозябры (искаженные символы) вместо кириллицы. Это происходит из-за неправильной настройки кодировки консоли.

## Решение

### Вариант 1: Автоматическая настройка (рекомендуется)

#### Если получаете ошибку "выполнение сценариев отключено":

**Способ А: Запуск с обходом политики (для одного раза)**
```powershell
powershell -ExecutionPolicy Bypass -File .\setup-powershell-encoding.ps1
```

**Способ Б: Временно изменить политику выполнения**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\setup-powershell-encoding.ps1
```

**Способ В: Разблокировать файл**
```powershell
Unblock-File -Path .\setup-powershell-encoding.ps1
.\setup-powershell-encoding.ps1
```

#### Обычный запуск (если политика уже настроена):

1. Откройте PowerShell (можно от имени администратора для глобальной настройки)
2. Выполните скрипт:
   ```powershell
   .\setup-powershell-encoding.ps1
   ```
3. Перезапустите PowerShell для применения настроек

### Вариант 2: Ручная настройка

#### Для текущей сессии PowerShell:

```powershell
# Установка кодировки UTF-8
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
chcp 65001
```

#### Для постоянной настройки (добавить в профиль):

1. Откройте профиль PowerShell:
   ```powershell
   notepad $PROFILE
   ```
   
   Если файл не существует, создайте его:
   ```powershell
   New-Item -Path $PROFILE -Type File -Force
   notepad $PROFILE
   ```

2. Добавьте следующие строки:
   ```powershell
   # Настройка кодировки UTF-8
   $OutputEncoding = [System.Text.Encoding]::UTF8
   [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
   [Console]::InputEncoding = [System.Text.Encoding]::UTF8
   $PSDefaultParameterValues['*:Encoding'] = 'utf8'
   chcp 65001 | Out-Null
   ```

3. Сохраните файл и перезапустите PowerShell

### Вариант 3: Настройка через реестр (требует прав администратора)

```powershell
# Установка кодовой страницы UTF-8 (65001) для консоли
Set-ItemProperty -Path "HKCU:\Console" -Name "CodePage" -Value 65001
```

## Проверка настройки

После применения настроек проверьте:

```powershell
Write-Host "Привет, мир! Тест кириллицы: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
```

Если текст отображается корректно, настройка выполнена успешно.

## Дополнительные настройки

### Настройка шрифта консоли

1. Щелкните правой кнопкой мыши по заголовку окна PowerShell
2. Выберите "Свойства" → вкладка "Шрифт"
3. Выберите шрифт, поддерживающий кириллицу (например, "Consolas" или "Lucida Console")

### Настройка для Windows Terminal (если используется)

Если вы используете Windows Terminal, добавьте в настройки (`settings.json`):

```json
{
    "profiles": {
        "defaults": {
            "font": {
                "face": "Cascadia Code"
            }
        },
        "list": [
            {
                "name": "PowerShell",
                "commandline": "powershell.exe",
                "font": {
                    "face": "Cascadia Code"
                }
            }
        ]
    }
}
```

## Решение проблем

### Если настройки не применяются:

1. Убедитесь, что вы перезапустили PowerShell после изменения профиля
2. Проверьте, что профиль загружается:
   ```powershell
   Test-Path $PROFILE
   ```
3. Проверьте содержимое профиля:
   ```powershell
   Get-Content $PROFILE
   ```

### Если проблема сохраняется:

1. Установите Windows Terminal из Microsoft Store (рекомендуется)
2. Используйте Git Bash для работы с bash-скриптами
3. Используйте WSL (Windows Subsystem for Linux) для запуска bash-скриптов

## Примечания

- Настройки профиля применяются только к новым сессиям PowerShell
- Для текущей сессии используйте команды из "Варианта 2"
- Некоторые старые приложения могут не поддерживать UTF-8

