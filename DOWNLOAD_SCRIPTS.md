# Инструкция по скачиванию скриптов развертывания

## Проблема: файл скачивается как HTML

Если при скачивании через браузер файл `deploy.sh` или `deploy_old_version.sh` скачивается как HTML, используйте один из способов ниже.

## Способ 1: Скачивание через raw URL (рекомендуется)

```bash
# Скачать deploy.sh
wget https://raw.githubusercontent.com/ZloyKeks/SupplyPlanCalculator/main/deploy.sh
chmod +x deploy.sh

# Скачать deploy_old_version.sh
wget https://raw.githubusercontent.com/ZloyKeks/SupplyPlanCalculator/main/deploy_old_version.sh
chmod +x deploy_old_version.sh
```

## Способ 2: Клонирование репозитория

```bash
# Клонировать весь репозиторий
git clone https://github.com/ZloyKeks/SupplyPlanCalculator.git
cd SupplyPlanCalculator
chmod +x deploy.sh deploy_old_version.sh
```

## Способ 3: Скачивание через curl

```bash
# Скачать deploy.sh
curl -O https://raw.githubusercontent.com/ZloyKeks/SupplyPlanCalculator/main/deploy.sh
chmod +x deploy.sh

# Скачать deploy_old_version.sh
curl -O https://raw.githubusercontent.com/ZloyKeks/SupplyPlanCalculator/main/deploy_old_version.sh
chmod +x deploy_old_version.sh
```

## Способ 4: Если файл уже скачан как HTML

Если файл уже скачан и имеет расширение `.html` или содержит HTML-код:

```bash
# Переименовать файл
mv deploy_old_version.sh.html deploy_old_version.sh

# Удалить HTML-обертку (если есть)
sed -i '1,/<pre>/d' deploy_old_version.sh
sed -i '/<\/pre>/,$d' deploy_old_version.sh

# Удалить HTML-сущности
sed -i 's/&lt;/</g' deploy_old_version.sh
sed -i 's/&gt;/>/g' deploy_old_version.sh
sed -i 's/&amp;/\&/g' deploy_old_version.sh

# Сделать исполняемым
chmod +x deploy_old_version.sh

# Проверить, что файл правильный
head -n 5 deploy_old_version.sh
# Должно показать: #!/bin/bash и комментарии
```

## Проверка файла

После скачивания проверьте, что файл правильный:

```bash
# Проверить первые строки
head -n 10 deploy_old_version.sh

# Должно быть:
# #!/bin/bash
# 
# # Скрипт для быстрого поднятия старой версии приложения на порту 8081
# ...

# Проверить тип файла
file deploy_old_version.sh
# Должно быть: deploy_old_version.sh: Bourne-Again shell script, ASCII text executable

# Проверить кодировку
file -bi deploy_old_version.sh
# Должно быть: text/plain; charset=utf-8
```

## Если файл все еще не работает

1. Убедитесь, что файл имеет правильные окончания строк (LF, не CRLF):
   ```bash
   dos2unix deploy_old_version.sh
   ```

2. Убедитесь, что нет BOM (Byte Order Mark):
   ```bash
   sed -i '1s/^\xEF\xBB\xBF//' deploy_old_version.sh
   ```

3. Проверьте синтаксис:
   ```bash
   bash -n deploy_old_version.sh
   ```

## Быстрое решение

Самый простой способ - использовать wget или curl с raw URL:

```bash
cd /tmp
wget https://raw.githubusercontent.com/ZloyKeks/SupplyPlanCalculator/main/deploy_old_version.sh
chmod +x deploy_old_version.sh
sudo mv deploy_old_version.sh /usr/local/bin/  # или в нужную директорию
```

