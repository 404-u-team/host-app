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

Демонстрационные данные не создаются. Зарегистрируйте пользователя через CLI или Desktop.
Для создания администратора задайте `ADMIN_BOOTSTRAP_PASSWORD` в окружении backend
(12–255 символов) перед запуском. Затем войдите с логином `ADMIN` и этим паролем;
подтверждение email для него не требуется. Существующий аккаунт не изменяется.

Заявка создается в `CREATED`. Администратор переводит ее прямо в `COMPLETED`:
в одной транзакции создается сервер с CPU, RAM, диском и ОС из заявки,
закрепленный за ее владельцем. `APPROVED` пока не используется.

Администратор видит все серверы. Поиск, фильтры, сортировка и экспорт XLSX
работают по заявкам всех пользователей для администратора и только по своим
заявкам для обычного пользователя.

При удалении сервера администратором все связанные с ним заявки удаляются
вместе с сервером в одной транзакции, включая выполненные заявки.

## CLI

В отдельном терминале из корня проекта:

```sh
cd host-app-cli
mvn clean package
mvn exec:java
```

## Настольный клиент JavaFX

После запуска backend и PostgreSQL в отдельном терминале из корня проекта:

```sh
cd host-app-desktop
mvn javafx:run
```

Клиент использует тот же backend, что и CLI. Подробности запуска, функции и отличие от требования JDBC в КР2 описаны в [host-app-desktop/README.md](host-app-desktop/README.md).

## Сборка backend

Из каталога `host-app-backend`:

```sh
mvn clean package
```
