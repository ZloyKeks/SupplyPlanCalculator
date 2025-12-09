#!/bin/bash

# Скрипт для быстрого поднятия старой версии приложения на порту 8081
# Использование: ./deploy_old_version.sh [опции]

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Конфигурация
APP_NAME="SupplyPlanCalculator"
APP_USER="supplycalc"
APP_DIR_OLD="/opt/${APP_NAME}-old"
SERVICE_NAME="${APP_NAME,,}-old"
GIT_REPO="https://github.com/ZloyKeks/SupplyPlanCalculator.git"
OLD_LIB_VERSION="1.2.17"
APP_PORT="8081"
LOG_DIR="/var/log/${APP_NAME}-old"
BRANCH_OLD="${BRANCH_OLD:-main}"  # Можно указать через переменную окружения или параметр
COMMIT_OLD="${COMMIT_OLD:-}"  # Можно указать конкретный коммит

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_root() {
    if [ "$EUID" -ne 0 ]; then
        log_error "Пожалуйста, запустите скрипт с правами root (sudo)"
        exit 1
    fi
}

# Откат к старой версии библиотеки (опционально, если нужно изменить версию в старой ветке)
revert_to_old_version() {
    log_info "Проверка версии библиотеки..."
    cd "$APP_DIR_OLD"
    
    # Проверяем текущую версию в pom.xml
    CURRENT_VERSION=$(grep -oP '<packing.version>\K[^<]+' pom.xml 2>/dev/null || echo "")
    
    if [ -n "$CURRENT_VERSION" ] && [ "$CURRENT_VERSION" != "$OLD_LIB_VERSION" ]; then
        log_info "Изменение версии библиотеки с $CURRENT_VERSION на ${OLD_LIB_VERSION}..."
        sed -i "s/<packing.version>.*<\/packing.version>/<packing.version>${OLD_LIB_VERSION}<\/packing.version>/" pom.xml
        
        # Удаление зависимости api, если есть
        if grep -q '<artifactId>api</artifactId>' pom.xml; then
            log_info "Удаление зависимости api..."
            sed -i '/<artifactId>api<\/artifactId>/,/<\/dependency>/d' pom.xml
        fi
    else
        log_info "Версия библиотеки уже соответствует или не найдена: $CURRENT_VERSION"
    fi
    
    # Откат изменений в коде к старой версии API
    log_info "Откат изменений в коде..."
    
    # SupplyPlanCalculatorUtil.java - откат к старому API
    if [ -f "src/main/java/by/legan/gt_tss/supplyplancalculator/servvice/SupplyPlanCalculatorUtil.java" ]; then
        log_info "Откат SupplyPlanCalculatorUtil.java..."
        # Заменяем импорты
        sed -i 's/com\.github\.skjolber\.packing\.api\.\*/com.github.skjolber.packing.*/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/servvice/SupplyPlanCalculatorUtil.java
        sed -i 's/com\.github\.skjolber\.packing\.packer\.laff\.LargestAreaFitFirstPackager/com.github.skjolber.packing.LargestAreaFitFirstPackager/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/servvice/SupplyPlanCalculatorUtil.java
        
        # Откат методов packToOneContainer и packToManyContainers
        cat > /tmp/old_pack_one.txt << 'EOFPACKONE'
    private Container packToOneContainer(String containerName, Dimension dimensions_container, int maxWeight , List<BoxItem> products) {
        List<Container> containers = new ArrayList<>();
        Container container = new Container(containerName, dimensions_container.getWidth(), dimensions_container.getDepth(), dimensions_container.getHeight(), maxWeight);
        containers.add(container);
        LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true, true, 1);
        return packager.pack(products, Long.MAX_VALUE);
    }
EOFPACKONE

        cat > /tmp/old_pack_many.txt << 'EOFPACKMANY'
    private List<Container> packToManyContainers(String prefix, Dimension dimensions_container, int maxWeight, List<BoxItem> products, int maxContainers){
        if (maxContainers == 0) maxContainers = 100;
        List<Container> containers = new ArrayList<>(maxContainers);
        for (int i = 0; i < maxContainers; i++) {
            Container container = new Container(dimensions_container, maxWeight);
            containers.add(container);
        }
        LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true, true, 0);
        return packager.packList(products, maxContainers, Long.MAX_VALUE);
    }
EOFPACKMANY

        # Заменяем методы (упрощенная версия - может потребоваться ручная правка)
        log_warn "Может потребоваться ручная правка методов packToOneContainer и packToManyContainers"
    fi
    
    # ContainerProjection.java - откат к старому API
    if [ -f "src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java" ]; then
        log_info "Откат ContainerProjection.java..."
        sed -i 's/com\.github\.skjolber\.packing\.api\./com.github.skjolber.packing./g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
        sed -i 's/getStack()/getLevels()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
        sed -i 's/getDx()/getWidth()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
        sed -i 's/getDy()/getDepth()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
        sed -i 's/getDz()/getHeight()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
        sed -i 's/placement\.getBoxItem()\.getBox()/placement.getBox()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
        sed -i 's/box\.getStackValue(0)\.getDx()/box.getWidth()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
        sed -i 's/box\.getStackValue(0)\.getDy()/box.getDepth()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
        sed -i 's/box\.getStackValue(0)\.getDz()/box.getHeight()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/visualization/ContainerProjection.java
    fi
    
    # ExcelSupplyPlanUtils.java - откат к старому API
    if [ -f "src/main/java/by/legan/gt_tss/supplyplancalculator/servvice/ExcelSupplyPlanUtils.java" ]; then
        log_info "Откат ExcelSupplyPlanUtils.java..."
        sed -i 's/com\.github\.skjolber\.packing\.api\./com.github.skjolber.packing./g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/servvice/ExcelSupplyPlanUtils.java
        sed -i 's/getStack()/getLevels()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/servvice/ExcelSupplyPlanUtils.java
        sed -i 's/placement\.getBoxItem()\.getBox()/placement.getBox()/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/servvice/ExcelSupplyPlanUtils.java
    fi
    
    # Data классы - откат импортов
    find src/main/java/by/legan/gt_tss/supplyplancalculator/Data -name "*.java" -exec sed -i 's/com\.github\.skjolber\.packing\.api\./com.github.skjolber.packing./g' {} \;
    
    # Удаляем Dimension.java если он есть (в старой версии используется из библиотеки)
    if [ -f "src/main/java/by/legan/gt_tss/supplyplancalculator/Data/Dimension.java" ]; then
        log_info "Удаление Dimension.java (используется из библиотеки)..."
        rm -f src/main/java/by/legan/gt_tss/supplyplancalculator/Data/Dimension.java
        # Обновляем импорты в Item.java
        sed -i 's/import by\.legan\.gt_tss\.supplyplancalculator\.Data\.Dimension;/import com.github.skjolber.packing.Dimension;/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/Data/Item.java
        sed -i 's/Dimension dimensions_container = new Dimension();/com.github.skjolber.packing.Dimension dimensions_container = new Dimension();/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/Data/Item.java
        sed -i 's/Dimension dimensions_item = new Dimension();/com.github.skjolber.packing.Dimension dimensions_item = new Dimension();/g' \
            src/main/java/by/legan/gt_tss/supplyplancalculator/Data/Item.java
    fi
    
    log_info "Откат к старой версии завершен"
}

# Клонирование/обновление репозитория
clone_or_update_repo() {
    log_info "Работа с репозиторием..."
    
    if [ -d "$APP_DIR_OLD/.git" ]; then
        log_info "Обновление репозитория..."
        cd "$APP_DIR_OLD"
        sudo -u "$APP_USER" git fetch origin --all --tags
        
        # Переключение на указанную ветку или коммит
        if [ -n "$COMMIT_OLD" ]; then
            log_info "Переключение на коммит: $COMMIT_OLD"
            sudo -u "$APP_USER" git checkout "$COMMIT_OLD"
        else
            log_info "Переключение на ветку: $BRANCH_OLD"
            sudo -u "$APP_USER" git checkout "$BRANCH_OLD" 2>/dev/null || \
            sudo -u "$APP_USER" git checkout -b "$BRANCH_OLD" "origin/$BRANCH_OLD"
            sudo -u "$APP_USER" git reset --hard "origin/$BRANCH_OLD"
        fi
        
        sudo -u "$APP_USER" git clean -fd
    else
        log_info "Клонирование репозитория..."
        if [ -d "$APP_DIR_OLD" ]; then
            rm -rf "$APP_DIR_OLD"
        fi
        git clone "$GIT_REPO" "$APP_DIR_OLD"
        chown -R "$APP_USER:$APP_USER" "$APP_DIR_OLD"
        
        cd "$APP_DIR_OLD"
        
        # Переключение на указанную ветку или коммит
        if [ -n "$COMMIT_OLD" ]; then
            log_info "Переключение на коммит: $COMMIT_OLD"
            sudo -u "$APP_USER" git checkout "$COMMIT_OLD"
        else
            log_info "Переключение на ветку: $BRANCH_OLD"
            sudo -u "$APP_USER" git checkout "$BRANCH_OLD" 2>/dev/null || \
            sudo -u "$APP_USER" git checkout -b "$BRANCH_OLD" "origin/$BRANCH_OLD"
        fi
    fi
    
    # Показываем текущий коммит
    CURRENT_COMMIT=$(cd "$APP_DIR_OLD" && sudo -u "$APP_USER" git rev-parse --short HEAD)
    CURRENT_BRANCH=$(cd "$APP_DIR_OLD" && sudo -u "$APP_USER" git branch --show-current 2>/dev/null || echo "detached HEAD")
    log_info "Текущий коммит: $CURRENT_COMMIT (ветка: $CURRENT_BRANCH)"
    log_info "Репозиторий готов"
}

# Сборка приложения
build_app() {
    log_info "Сборка приложения..."
    cd "$APP_DIR_OLD"
    sudo -u "$APP_USER" mvn clean package -DskipTests
    
    JAR_FILE="$APP_DIR_OLD/target/${APP_NAME}-0.0.1-SNAPSHOT.jar"
    if [ -f "$JAR_FILE" ]; then
        log_info "Приложение успешно собрано: $JAR_FILE"
        chown "$APP_USER:$APP_USER" "$JAR_FILE"
    else
        log_error "JAR файл не найден после сборки"
        exit 1
    fi
}

# Настройка systemd service для порта 8081
setup_service() {
    log_info "Настройка systemd service на порту $APP_PORT..."
    mkdir -p "$LOG_DIR"
    chown "$APP_USER:$APP_USER" "$LOG_DIR"
    
    SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
    cat > "$SERVICE_FILE" << EOF
[Unit]
Description=SupplyPlanCalculator Application (Old Version on port $APP_PORT)
After=network.target

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR_OLD}
ExecStart=/usr/bin/java -Dserver.port=${APP_PORT} -jar ${APP_DIR_OLD}/target/${APP_NAME}-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=append:${LOG_DIR}/app.log
StandardError=append:${LOG_DIR}/error.log
Environment="JAVA_OPTS=-Xms512m -Xmx1024m"

[Install]
WantedBy=multi-user.target
EOF
    
    systemctl daemon-reload
    log_info "Systemd service настроен: $SERVICE_FILE"
}

# Развертывание приложения
deploy_app() {
    log_info "Развертывание приложения на порту $APP_PORT..."
    
    if systemctl is-active --quiet "$SERVICE_NAME"; then
        log_info "Остановка старого приложения..."
        systemctl stop "$SERVICE_NAME"
        sleep 2
    fi
    
    log_info "Запуск приложения..."
    systemctl start "$SERVICE_NAME"
    sleep 3
    
    if systemctl is-active --quiet "$SERVICE_NAME"; then
        log_info "Приложение успешно запущено на порту $APP_PORT"
        systemctl enable "$SERVICE_NAME"
        log_info "Автозапуск включен"
    else
        log_error "Ошибка запуска приложения"
        systemctl status "$SERVICE_NAME"
        exit 1
    fi
}

# Показать статус
show_status() {
    echo ""
    log_info "=== Статус приложения (порт $APP_PORT) ==="
    systemctl status "$SERVICE_NAME" --no-pager -l || true
    
    echo ""
    log_info "=== Проверка порта $APP_PORT ==="
    if command -v netstat &> /dev/null && netstat -tuln | grep -q ":$APP_PORT "; then
        log_info "Порт $APP_PORT открыт"
    elif command -v ss &> /dev/null && ss -tuln | grep -q ":$APP_PORT "; then
        log_info "Порт $APP_PORT открыт"
    else
        log_warn "Порт $APP_PORT не прослушивается"
    fi
}

# Полное развертывание
full_deploy() {
    log_info "=== Начало развертывания старой версии на порту $APP_PORT ==="
    check_root
    clone_or_update_repo
    revert_to_old_version
    build_app
    setup_service
    deploy_app
    show_status
    log_info "=== Развертывание завершено ==="
    log_info "Приложение доступно на: http://localhost:$APP_PORT"
}

main() {
    ACTION=""
    
    # Парсинг параметров для ветки и коммита
    while [[ $# -gt 0 ]]; do
        case $1 in
            --branch)
                BRANCH_OLD="$2"
                shift 2
                ;;
            --commit)
                COMMIT_OLD="$2"
                shift 2
                ;;
            --revert)
                ACTION="revert"
                shift
                ;;
            --build)
                ACTION="build"
                shift
                ;;
            --setup)
                ACTION="setup"
                shift
                ;;
            --deploy)
                ACTION="deploy"
                shift
                ;;
            --status)
                ACTION="status"
                shift
                ;;
            --full)
                ACTION="full"
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "Неизвестный параметр: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Выполнение действия
    case "$ACTION" in
        revert)
            check_root
            clone_or_update_repo
            revert_to_old_version
            ;;
        build)
            check_root
            build_app
            ;;
        setup)
            check_root
            setup_service
            ;;
        deploy)
            check_root
            deploy_app
            ;;
        status)
            show_status
            ;;
        full)
            full_deploy
            ;;
        *)
            show_help
            exit 1
            ;;
    esac
}

show_help() {
    echo "Использование: $0 [опция] [--branch ВЕТКА] [--commit КОММИТ]"
    echo ""
    echo "Опции:"
    echo "  --full           Полное развертывание (клонирование, сборка, запуск)"
    echo "  --revert         Откатить код к старой версии библиотеки"
    echo "  --build          Собрать приложение"
    echo "  --setup          Настроить systemd service"
    echo "  --deploy         Развернуть приложение"
    echo "  --status         Показать статус"
    echo "  --help, -h       Показать эту справку"
    echo ""
    echo "Параметры ветки/коммита:"
    echo "  --branch ВЕТКА   Указать ветку для развертывания (по умолчанию: main)"
    echo "  --commit КОММИТ  Указать коммит для развертывания (приоритет над --branch)"
    echo ""
    echo "Примеры:"
    echo "  # Развернуть ветку old-version"
    echo "  sudo $0 --full --branch old-version"
    echo ""
    echo "  # Развернуть конкретный коммит"
    echo "  sudo $0 --full --commit fb2bf0b"
    echo ""
    echo "  # Развернуть ветку через переменную окружения"
    echo "  BRANCH_OLD=old-version sudo $0 --full"
    echo ""
    echo "  # Только клонирование и переключение на ветку"
    echo "  sudo $0 --revert --branch old-version"
    echo ""
    echo "  # Проверка статуса"
    echo "  $0 --status"
}

main "$@"

