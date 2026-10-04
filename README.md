# Host App

## Backend в Docker Compose

Полный запуск backend и PostgreSQL из каталога `host-app-backend`:

```sh
cd host-app-backend
docker compose up --build
```

API: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html

Для локальной разработки с запуском Spring Boot через Maven используйте только PostgreSQL из каталога `host-app-backend`:

```sh
docker compose -f docker-compose.dev.yaml up -d
./mvnw spring-boot:run
```

Демонстрационные данные не создаются. Зарегистрируйте пользователя через CLI.
Для администратора используйте логин `ADMIN`.
Ранее созданные демопользователи `demo1@hostapp.local`–`demo5@hostapp.local`
и их заявки удаляются при запуске backend.

Заявка создается в `CREATED`. Администратор переводит ее прямо в `COMPLETED`:
в одной транзакции создается сервер с CPU, RAM, диском и ОС из заявки,
закрепленный за ее владельцем. `APPROVED` пока не используется.

## CLI

В отдельном терминале из корня проекта:

```sh
cd host-app-cli
mvn clean package
mvn exec:java
```

## Сборка backend

Из каталога `host-app-backend`:

```sh
mvn clean package
```
