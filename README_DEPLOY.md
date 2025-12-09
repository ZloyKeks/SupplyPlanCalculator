# Инструкция по развертыванию SupplyPlanCalculator на Ubuntu Server 24

## Быстрый старт

Для полного развертывания выполните:

```bash
sudo ./deploy.sh --full
```

## Пошаговое развертывание

### 1. Установка зависимостей

```bash
sudo ./deploy.sh --install-deps
```

Это установит:
- Java 21 (OpenJDK)
- Maven
- Git и другие необходимые инструменты

### 2. Сборка приложения

```bash
sudo ./deploy.sh --build
```

Скрипт автоматически:
- Создаст пользователя `supplycalc`
- Склонирует/обновит репозиторий
- Соберет JAR файл

### 3. Настройка systemd service

```bash
sudo ./deploy.sh --setup-service
```

Создаст systemd unit файл для автозапуска приложения.

### 4. Развертывание и запуск

```bash
sudo ./deploy.sh --deploy
```

## Управление приложением

### Проверка статуса

```bash
./deploy.sh --status
# или
sudo systemctl status supplyplancalculator
```

### Просмотр логов

```bash
./deploy.sh --logs
# или
sudo journalctl -u supplyplancalculator -f
# или
tail -f /var/log/SupplyPlanCalculator/app.log
```

### Перезапуск

```bash
sudo ./deploy.sh --restart
# или
sudo systemctl restart supplyplancalculator
```

### Остановка

```bash
sudo systemctl stop supplyplancalculator
```

### Запуск

```bash
sudo systemctl start supplyplancalculator
```

## Конфигурация

По умолчанию скрипт использует следующие настройки:

- **Пользователь**: `supplycalc`
- **Директория приложения**: `/opt/SupplyPlanCalculator`
- **Порт**: `8080`
- **Логи**: `/var/log/SupplyPlanCalculator/`
- **Service name**: `supplyplancalculator`

Для изменения настроек отредактируйте переменные в начале файла `deploy.sh`:

```bash
APP_USER="supplycalc"
APP_DIR="/opt/${APP_NAME}"
APP_PORT="8080"
LOG_DIR="/var/log/${APP_NAME}"
```

## Настройка Nginx (опционально)

Для использования Nginx как reverse proxy создайте файл `/etc/nginx/sites-available/supplyplancalculator`:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Активируйте конфигурацию:

```bash
sudo ln -s /etc/nginx/sites-available/supplyplancalculator /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## Настройка firewall

Если используется UFW:

```bash
sudo ufw allow 8080/tcp
# или для Nginx
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

## Обновление приложения

Для обновления приложения:

```bash
sudo ./deploy.sh --build
sudo ./deploy.sh --restart
```

Или полное обновление:

```bash
sudo ./deploy.sh --full
```

## Устранение неполадок

### Приложение не запускается

1. Проверьте логи:
   ```bash
   ./deploy.sh --logs
   ```

2. Проверьте статус:
   ```bash
   ./deploy.sh --status
   ```

3. Проверьте Java:
   ```bash
   java -version
   ```

4. Проверьте порт:
   ```bash
   sudo netstat -tuln | grep 8080
   ```

### Проблемы с правами доступа

Убедитесь, что пользователь `supplycalc` имеет права на директорию:

```bash
sudo chown -R supplycalc:supplycalc /opt/SupplyPlanCalculator
sudo chown -R supplycalc:supplycalc /var/log/SupplyPlanCalculator
```

### Проблемы с памятью

Отредактируйте `/etc/systemd/system/supplyplancalculator.service` и измените `JAVA_OPTS`:

```ini
Environment="JAVA_OPTS=-Xms256m -Xmx512m"
```

Затем:

```bash
sudo systemctl daemon-reload
sudo systemctl restart supplyplancalculator
```

## Безопасность

1. **Не запускайте приложение от root** - скрипт автоматически создает пользователя `supplycalc`

2. **Настройте firewall** - откройте только необходимые порты

3. **Используйте HTTPS** - настройте SSL сертификат через Let's Encrypt

4. **Регулярно обновляйте систему**:
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```

## Мониторинг

Для мониторинга использования ресурсов:

```bash
# CPU и память
top -p $(pgrep -f SupplyPlanCalculator)

# Логи в реальном времени
sudo journalctl -u supplyplancalculator -f
```

## Резервное копирование

Рекомендуется настроить резервное копирование:

```bash
# Резервная копия конфигурации и данных
sudo tar -czf backup-$(date +%Y%m%d).tar.gz \
    /opt/SupplyPlanCalculator \
    /var/log/SupplyPlanCalculator \
    /etc/systemd/system/supplyplancalculator.service
```

