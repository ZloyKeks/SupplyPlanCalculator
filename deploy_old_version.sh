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
BRANCH_OLD="main"  # Можно указать конкретную ветку или коммит

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

# Откат к старой версии библиотеки
revert_to_old_version() {
    log_info "Откат к старой версии библиотеки ${OLD_LIB_VERSION}..."
    cd "$APP_DIR_OLD"
    
    # Изменение версии в pom.xml
    log_info "Обновление pom.xml..."
    sed -i "s/<packing.version>.*<\/packing.version>/<packing.version>${OLD_LIB_VERSION}<\/packing.version>/" pom.xml
    
    # Удаление зависимости api, если есть
    sed -i '/<artifactId>api<\/artifactId>/,/<\/dependency>/d' pom.xml
    
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
        sudo -u "$APP_USER" git fetch origin
        sudo -u "$APP_USER" git reset --hard origin/main
        sudo -u "$APP_USER" git clean -fd
    else
        log_info "Клонирование репозитория..."
        if [ -d "$APP_DIR_OLD" ]; then
            rm -rf "$APP_DIR_OLD"
        fi
        git clone "$GIT_REPO" "$APP_DIR_OLD"
        chown -R "$APP_USER:$APP_USER" "$APP_DIR_OLD"
    fi
    
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
    case "${1:-}" in
        --revert)
            check_root
            clone_or_update_repo
            revert_to_old_version
            ;;
        --build)
            check_root
            build_app
            ;;
        --setup)
            check_root
            setup_service
            ;;
        --deploy)
            check_root
            deploy_app
            ;;
        --status)
            show_status
            ;;
        --full)
            full_deploy
            ;;
        *)
            echo "Использование: $0 [опция]"
            echo ""
            echo "Опции:"
            echo "  --revert    Откатить код к старой версии библиотеки"
            echo "  --build     Собрать приложение"
            echo "  --setup     Настроить systemd service"
            echo "  --deploy    Развернуть приложение"
            echo "  --status    Показать статус"
            echo "  --full      Полное развертывание (все вышеперечисленное)"
            echo ""
            echo "Примеры:"
            echo "  sudo $0 --full      # Полное развертывание"
            echo "  sudo $0 --revert    # Только откат кода"
            echo "  $0 --status         # Статус (без root)"
            exit 1
            ;;
    esac
}

main "$@"

