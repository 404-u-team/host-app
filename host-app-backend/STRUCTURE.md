# Структура бэкенда

| Пакет | Содержимое |
| --- | --- |
| `controller` | Существующие REST-маршруты и API КР №3 |
| `service` | Бизнес-операции, транзакции, почта и аудит |
| `repository` | Доступ к данным через Spring Data JPA |
| `domain` | JPA-сущности и перечисления |
| `dto` | Входные и выходные модели |
| `security` | Проверка паролей, JWT и поддержка прежних сессий |
| `filter` | Correlation ID, поиск, фильтрация и параметры страниц |
| `error` | Единый формат HTTP-ошибок |
| `config` | Spring Security и OpenAPI |
| `src/main/resources/db/migration` | SQL-миграции Flyway |
