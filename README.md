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

При первом запуске создаются демонстрационные пользователи и заявки. Для входа:

- Email: `demo1@hostapp.local`
- Пароль: `demo1234`

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
