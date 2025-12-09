#!/bin/bash

# Скрипт развертывания SupplyPlanCalculator на Ubuntu Server 24
# Использование: ./deploy.sh [опции]
# Опции:
#   --install-deps    - установить зависимости (Java, Maven)
#   --setup-service   - настроить systemd service
#   --build           - собрать приложение
#   --deploy          - развернуть приложение
#   --restart         - перезапустить приложение
#   --status          - показать статус
#   --logs            - показать логи
#   --full            - полное развертывание (все вышеперечисленное)

set -e  # Остановка при ошибке

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Конфигурация
APP_NAME="SupplyPlanCalculator"
APP_USER="supplycalc"
APP_DIR="/opt/${APP_NAME}"
SERVICE_NAME="${APP_NAME,,}"  # lowercase
GIT_REPO="https://github.com/ZloyKeks/SupplyPlanCalculator.git"
JAVA_VERSION="21"
MAVEN_VERSION="3.9.11"  # Последняя стабильная версия (в репозиториях Ubuntu 24 может быть 3.9.6)
APP_PORT="8080"
LOG_DIR="/var/log/${APP_NAME}"

# Функция для вывода сообщений
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверка прав root
check_root() {
    if [ "$EUID" -ne 0 ]; then 
        log_error "Пожалуйста, запустите скрипт с правами root (sudo)"
        exit 1
    fi
}

# Установка зависимостей
install_dependencies() {
    log_info "Установка зависимостей..."
    
    # Обновление пакетов
    apt-get update
    
    # Установка необходимых пакетов
    apt-get install -y \
        curl \
        wget \
        git \
        unzip \
        software-properties-common
    
    # Установка Java 21
    log_info "Установка Java ${JAVA_VERSION}..."
    apt-get install -y openjdk-${JAVA_VERSION}-jdk
    
    # Проверка установки Java
    if command -v java &> /dev/null; then
        JAVA_VER=$(java -version 2>&1 | head -n 1)
        log_info "Java установлена: $JAVA_VER"
    else
        log_error "Ошибка установки Java"
        exit 1
    fi
    
    # Установка Maven
    log_info "Установка Maven..."
    
    # Проверка версии Maven в репозиториях
    AVAILABLE_MVN=$(apt-cache madison maven | head -n 1 | awk '{print $3}' | cut -d'-' -f1)
    log_info "Доступная версия Maven в репозиториях: $AVAILABLE_MVN"
    
    # Установка из репозиториев
    apt-get install -y maven
    
    # Проверка установки Maven
    if command -v mvn &> /dev/null; then
        INSTALLED_MVN=$(mvn -version | head -n 1 | awk '{print $3}')
        log_info "Maven установлен: версия $INSTALLED_MVN"
        
        # Предупреждение, если версия старая
        if [[ "$INSTALLED_MVN" < "3.9.6" ]]; then
            log_warn "Установлена старая версия Maven ($INSTALLED_MVN)"
            log_warn "Для установки последней версии (3.9.11) используйте ручную установку:"
            log_warn "  wget https://dlcdn.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz"
            log_warn "  tar -xzf apache-maven-3.9.11-bin.tar.gz -C /opt"
            log_warn "  export PATH=/opt/apache-maven-3.9.11/bin:\$PATH"
        fi
    else
        log_error "Ошибка установки Maven"
        exit 1
    fi
    
    log_info "Зависимости установлены успешно"
}

# Создание пользователя для приложения
create_app_user() {
    if id "$APP_USER" &>/dev/null; then
        log_info "Пользователь $APP_USER уже существует"
    else
        log_info "Создание пользователя $APP_USER..."
        useradd -r -s /bin/bash -d "$APP_DIR" -m "$APP_USER"
        log_info "Пользователь $APP_USER создан"
    fi
}

# Клонирование/обновление репозитория
clone_or_update_repo() {
    log_info "Работа с репозиторием..."
    
    if [ -d "$APP_DIR/.git" ]; then
        log_info "Обновление репозитория..."
        cd "$APP_DIR"
        sudo -u "$APP_USER" git fetch origin
        sudo -u "$APP_USER" git reset --hard origin/main
        sudo -u "$APP_USER" git clean -fd
    else
        log_info "Клонирование репозитория..."
        if [ -d "$APP_DIR" ]; then
            rm -rf "$APP_DIR"
        fi
        git clone "$GIT_REPO" "$APP_DIR"
        chown -R "$APP_USER:$APP_USER" "$APP_DIR"
    fi
    
    log_info "Репозиторий готов"
}

# Сборка приложения
build_app() {
    log_info "Сборка приложения..."
    cd "$APP_DIR"
    
    # Очистка и сборка
    sudo -u "$APP_USER" mvn clean package -DskipTests
    
    # Проверка наличия JAR файла
    JAR_FILE="$APP_DIR/target/${APP_NAME}-0.0.1-SNAPSHOT.jar"
    if [ -f "$JAR_FILE" ]; then
        log_info "Приложение успешно собрано: $JAR_FILE"
        chown "$APP_USER:$APP_USER" "$JAR_FILE"
    else
        log_error "JAR файл не найден после сборки"
        exit 1
    fi
}

# Настройка systemd service
setup_service() {
    log_info "Настройка systemd service..."
    
    # Создание директории для логов
    mkdir -p "$LOG_DIR"
    chown "$APP_USER:$APP_USER" "$LOG_DIR"
    
    # Создание systemd unit файла
    SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
    cat > "$SERVICE_FILE" << EOF
[Unit]
Description=SupplyPlanCalculator Application
After=network.target

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR}
ExecStart=/usr/bin/java -jar ${APP_DIR}/target/${APP_NAME}-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=append:${LOG_DIR}/app.log
StandardError=append:${LOG_DIR}/error.log
Environment="JAVA_OPTS=-Xms512m -Xmx1024m"

[Install]
WantedBy=multi-user.target
EOF
    
    # Перезагрузка systemd
    systemctl daemon-reload
    log_info "Systemd service настроен: $SERVICE_FILE"
}

# Развертывание приложения
deploy_app() {
    log_info "Развертывание приложения..."
    
    # Остановка старого приложения если запущено
    if systemctl is-active --quiet "$SERVICE_NAME"; then
        log_info "Остановка старого приложения..."
        systemctl stop "$SERVICE_NAME"
        sleep 2
    fi
    
    # Запуск нового приложения
    log_info "Запуск приложения..."
    systemctl start "$SERVICE_NAME"
    sleep 3
    
    # Проверка статуса
    if systemctl is-active --quiet "$SERVICE_NAME"; then
        log_info "Приложение успешно запущено"
        systemctl enable "$SERVICE_NAME"
        log_info "Автозапуск включен"
    else
        log_error "Ошибка запуска приложения"
        systemctl status "$SERVICE_NAME"
        exit 1
    fi
}

# Перезапуск приложения
restart_app() {
    log_info "Перезапуск приложения..."
    systemctl restart "$SERVICE_NAME"
    sleep 3
    
    if systemctl is-active --quiet "$SERVICE_NAME"; then
        log_info "Приложение успешно перезапущено"
    else
        log_error "Ошибка перезапуска приложения"
        systemctl status "$SERVICE_NAME"
        exit 1
    fi
}

# Показать статус
show_status() {
    echo ""
    log_info "=== Статус приложения ==="
    systemctl status "$SERVICE_NAME" --no-pager -l
    
    echo ""
    log_info "=== Проверка порта $APP_PORT ==="
    if netstat -tuln | grep -q ":$APP_PORT "; then
        log_info "Порт $APP_PORT открыт"
    else
        log_warn "Порт $APP_PORT не прослушивается"
    fi
    
    echo ""
    log_info "=== Использование ресурсов ==="
    ps aux | grep -i "${APP_NAME}" | grep -v grep || log_warn "Процесс не найден"
}

# Показать логи
show_logs() {
    log_info "Последние 50 строк логов приложения:"
    echo ""
    tail -n 50 "$LOG_DIR/app.log" 2>/dev/null || log_warn "Лог файл не найден"
    echo ""
    log_info "Последние 50 строк ошибок:"
    echo ""
    tail -n 50 "$LOG_DIR/error.log" 2>/dev/null || log_warn "Лог файл ошибок не найден"
}

# Полное развертывание
full_deploy() {
    log_info "=== Начало полного развертывания ==="
    check_root
    install_dependencies
    create_app_user
    clone_or_update_repo
    build_app
    setup_service
    deploy_app
    show_status
    log_info "=== Развертывание завершено ==="
}

# Главное меню
main() {
    case "${1:-}" in
        --install-deps)
            check_root
            install_dependencies
            ;;
        --setup-service)
            check_root
            setup_service
            ;;
        --build)
            check_root
            create_app_user
            clone_or_update_repo
            build_app
            ;;
        --deploy)
            check_root
            deploy_app
            ;;
        --restart)
            check_root
            restart_app
            ;;
        --status)
            show_status
            ;;
        --logs)
            show_logs
            ;;
        --full)
            full_deploy
            ;;
        *)
            echo "Использование: $0 [опция]"
            echo ""
            echo "Опции:"
            echo "  --install-deps    Установить зависимости (Java, Maven)"
            echo "  --setup-service   Настроить systemd service"
            echo "  --build           Собрать приложение"
            echo "  --deploy          Развернуть приложение"
            echo "  --restart         Перезапустить приложение"
            echo "  --status          Показать статус"
            echo "  --logs            Показать логи"
            echo "  --full            Полное развертывание (все вышеперечисленное)"
            echo ""
            echo "Примеры:"
            echo "  sudo $0 --full           # Полное развертывание"
            echo "  sudo $0 --build          # Только сборка"
            echo "  sudo $0 --restart        # Перезапуск"
            echo "  $0 --status              # Статус (без root)"
            echo "  $0 --logs                # Логи (без root)"
            exit 1
            ;;
    esac
}

main "$@"

