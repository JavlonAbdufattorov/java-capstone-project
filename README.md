# Smart Clinic Managemenrt System

## React frontend

Стартовая панель управления находится в [`frontend/`](frontend/README.md).
Архитектура и поток данных описаны в [`schema-architecture.md`](schema-architecture.md).

## Backend

Реализованы MVC-панели администратора и врача, REST API, JWT с ролью,
BCrypt-пароли, репозитории PostgreSQL и MongoDB, проверка владельца записей.

Для запуска нужны Java 17+, Maven, PostgreSQL (БД `cms`) и MongoDB.
Проект компилируется для Java 17. Для сборки и запуска именно на JDK 17
укажите путь к установленному JDK перед выполнением команд Maven:

```bash
export JAVA_HOME=/path/to/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version
```

В выводе `mvn -version` должна быть Java 17. В IDE также выберите JDK 17
для проекта и запуска Maven.

Настройки подключения находятся в `app/src/main/resources/application.properties`;
их можно переопределить через `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` и `SPRING_DATA_MONGODB_URI`.
Задайте `JWT_SECRET` — случайный секрет длиной минимум 32 байта.
Для создания первого администратора задайте `CLINIC_ADMIN_USERNAME` и
`CLINIC_ADMIN_PASSWORD` (минимум 12 символов). После первого запуска удалите
эти две переменные; существующая учётная запись не перезаписывается.
Пароли в БД должны храниться как BCrypt-хеши; старые открытые пароли нужно мигрировать.

```bash
mvn -f app/pom.xml spring-boot:run
mvn -f app/pom.xml test
```

Приложение: `http://localhost:8080`. Тесты используют H2; тест контекста
создаёт Mongo-клиент, но не выполняет операции с MongoDB.

| Метод | Путь | Назначение |
| --- | --- | --- |
| POST | `/admin/login` | Вход администратора: `username`, `password` |
| POST | `/doctor/login`, `/patient/login` | Вход: `email`, `password` |
| GET | `/adminDashboard/{token}`, `/doctorDashboard/{token}` | Thymeleaf-панель; недействительный токен перенаправляет на главную |
| GET | `/doctor` | Список врачей |
| GET | `/doctor/filter/{name}/{time}/{speciality}` | Фильтры; `time`: AM/PM, `all` отключает фильтр |
| GET | `/doctor/availability/{user}/{doctorId}/{date}/{token}` | Свободные часовые интервалы; дата YYYY-MM-DD |
| POST, PUT | `/doctor/{token}` | Создание/обновление врача администратором |
| DELETE | `/doctor/{id}/{token}` | Удаление врача и его записей |
| POST | `/patient` | Регистрация пациента |
| GET | `/patient/{token}` | Профиль пациента |
| GET | `/patient/{id}/{user}/{token}` | Записи пациента; врач видит только свои |
| GET | `/patient/filter/{condition}/{name}/{token}` | Фильтр: past (завершённые), future (запланированные), all |
| POST, PUT | `/appointments/{token}` | Запись/перенос пациентом |
| DELETE | `/appointments/{id}/{token}` | Отмена будущей записи её владельцем |
| GET | `/appointments/{date}/{patientName}/{token}` | Записи текущего врача за день |
| POST | `/prescription/{token}` | Рецепт от врача, которому принадлежит запись |
| GET | `/prescription/{appointmentId}/{token}` | Рецепт для записи текущего врача |

При записи передаются `doctor: {id}`, `patient: {id}`, `appointmentTime`
(ISO local datetime); при переносе также `id`. Статус и идентификатор новой записи
назначает сервер. Время сравнивается с локальным временем сервера.
Пароли не возвращаются в JSON. Ошибки содержат поле `message`.

PostgreSQL и MongoDB не используют общую транзакцию: если обновление статуса
после сохранения рецепта завершается ошибкой, сервис пытается удалить рецепт.
При аварийном завершении процесса между операциями потребуется сверка данных.
Удаление врача не удаляет архивные MongoDB-рецепты.
React-интерфейс в `frontend/` пока остаётся демонстрационным, а часть исходных
JS-файлов Thymeleaf содержит учебные заготовки; этот этап реализует бэкенд.

## Запуск через Docker

Из корня проекта выполните:

```bash
# Только при первом запуске, если .env ещё нет:
(umask 077; test -f .env || printf 'POSTGRES_PASSWORD=%s\nJWT_SECRET=%s\n' "$(openssl rand -hex 24)" "$(openssl rand -hex 32)" > .env)
docker compose up -d --build
```

Compose запускает backend, PostgreSQL и MongoDB. Приложение доступно на
http://localhost:8080. Если порт занят, добавьте `APP_PORT=8081` в `.env`.
Файл `.env` исключён из Git и контекста сборки. При необходимости добавьте
в него `CLINIC_ADMIN_USERNAME` и `CLINIC_ADMIN_PASSWORD` (минимум 12 символов)
для создания первого администратора; после создания удалите эти две настройки.

```bash
docker compose logs -f backend
docker compose down
```

Данные баз сохраняются в Docker volumes при остановке контейнеров.
