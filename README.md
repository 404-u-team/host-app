# Host App

## Веб-интерфейс и бэкенд КР №3

Для нового учебного окружения скопируйте корневой `.env.example` в `.env` и задайте пароль БД, временный пароль администратора и случайный Base64-ключ JWT длиной от 32 байт. На Windows выполните из корня проекта:

```powershell
powershell -ExecutionPolicy Bypass -File .\host-app-backend\start-compose.ps1
```

Та же сборка и запуск через Docker Compose:

```sh
docker compose --env-file .env -f host-app-backend/compose.yaml -f host-app-backend/compose.kr3.yaml up -d --build --wait
```

Откройте [http://localhost:8080/](http://localhost:8080/). На экране входа используйте логин `ADMIN` и временный пароль из `ADMIN_BOOTSTRAP_PASSWORD`. Веб-клиент предложит сменить пароль. Интерфейс включает реестр заявок, серверы и аналитику, а для администратора — настройки справочника, пользователей и журнал аудита. Swagger и OpenAPI доступны из профиля администратора.

Учебное окружение использует Java 21, PostgreSQL 17 и собственный том. PostgreSQL доступен на `localhost:55432`. Готовность проверяется по `GET /actuator/health`. [Контракт REST API и примеры запросов](host-app-backend/README.md).

Для разработки веб-клиента запустите API, затем из другого терминала выполните:

```sh
cd host-app-web
bun install --frozen-lockfile
bun run dev
```

Vite откроется на `http://localhost:5173/` и передаст запросы к локальному API. Если бэкенд запущен на другом адресе, задайте `HOST_APP_BACKEND_URL` перед запуском. Обычная сборка клиента — `bun run build` из `host-app-web`. Docker Compose собирает этот клиент и включает его в JAR Spring Boot, поэтому отдельный процесс Bun нужен только для разработки.

## Существующее окружение PostgreSQL 16

Запуск веб-клиента, бэкенда и PostgreSQL из каталога `host-app-backend`:

```sh
cd host-app-backend
docker compose up --build
```

- Веб-клиент — http://localhost:8080/
- API — http://localhost:8080/api
- Swagger — http://localhost:8080/swagger-ui.html

Для локальной разработки с запуском Spring Boot через Maven используйте только PostgreSQL из каталога `host-app-backend`:

```sh
docker compose -f docker-compose.dev.yaml up -d
./mvnw spring-boot:run
```

Демонстрационные данные не создаются. Зарегистрируйте пользователя через CLI или Desktop.
Для создания администратора задайте `ADMIN_BOOTSTRAP_PASSWORD` в окружении backend
(12–255 символов) перед запуском. Подтверждение email для `ADMIN` отключено. Смените временный пароль через API перед использованием аккаунта в CLI или Desktop. Существующий аккаунт не изменяется.

Заявка создается в `CREATED`. Администратор переводит ее прямо в `COMPLETED`:
в одной транзакции создается сервер с CPU, RAM, диском и ОС из заявки,
закрепленный за ее владельцем. `APPROVED` пока не используется.

Администратор видит все серверы. Поиск, фильтры, сортировка и экспорт XLSX
работают по заявкам всех пользователей для администратора и только по своим
заявкам для обычного пользователя.

В Desktop и CLI пользователь может запросить изменение параметров или отмену своего сервера.
Создаётся новая заявка типа `CHANGE` или `CANCEL` со статусом `CREATED`.
Администратор обрабатывает её как заявку на создание: `COMPLETED` выполняет действие,
`REJECTED` отклоняет, `CANCELLED` отменяет саму заявку без изменения сервера.
Изменение обновляет CPU, RAM, диск и ОС существующего сервера.
Одобренная отмена удаляет сервер и все связанные заявки, включая саму заявку на отмену.
Существующие заявки автоматически получают тип `CREATE` при обновлении схемы.

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
