# Host App

Для запуска нужны JDK 17, Maven и Docker Compose.

## PostgreSQL и backend

```sh
cd host-app-backend
docker compose -f docker-compose.dev.yaml up -d
mvn spring-boot:run
```

Swagger: http://localhost:8080/swagger-ui.html

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
