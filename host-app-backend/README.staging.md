# Стенд через Komodo и Nginx Proxy Manager

`compose.staging.yaml` — самостоятельный Compose-файл для стенда.
Используйте его отдельно, без объединения с `compose.yaml`: при объединении
могут сохраниться опубликованные порты локальной конфигурации.

Backend подключён к существующей Docker-сети `intranet` и доступен в ней
по постоянному DNS-имени `hostapp-staging-backend` на порту `8080`.
Nginx Proxy Manager должен быть подключён к той же сети на том же Docker-хосте.
Сеть `intranet` должна существовать до запуска стенда.

PostgreSQL подключён только к отдельной внутренней сети `database`.
Оба сервиса работают без публикации портов на хосте. У стенда свой volume
`postgres_data_staging`, не связанный с данными локального Compose.
Имя проекта по умолчанию — `hostapp-staging`; Komodo может переопределить его
именем Stack. Сохраняйте имя проекта при обновлениях, чтобы использовать тот же volume.

## Komodo

Создайте Stack с источником Git и укажите:

| Параметр | Значение |
| --- | --- |
| Server | Сервер, на котором работают Nginx Proxy Manager и сеть `intranet` |
| Repo / Branch | Репозиторий приложения и ветка с файлами стенда |
| Run directory | `.` (корень репозитория) |
| File paths | Только `host-app-backend/compose.staging.yaml` |
| Env file path | `.env` |
| Project name | `hostapp-staging` |
| Extra args | `--build` |

В Environment перенесите переменные из `.env.staging.example` и обязательно
задайте `POSTGRES_PASSWORD`. Реальные пароли храните в настройках стенда,
не в репозитории. Нажмите Deploy.

Пути File paths и Env file path отсчитываются от Run directory. Если в логе
`Missing files: compose.staging.yaml`, проверьте эту пару настроек: файл лежит
в подкаталоге `host-app-backend`, а не в корне репозитория.

### Первый вход администратора

Добавьте в Environment или секреты Stack переменную `ADMIN_BOOTSTRAP_PASSWORD`
с новым случайным паролем длиной от 12 до 255 символов и выполните Deploy.
При запуске backend создаст аккаунт с логином `ADMIN`. Для него не отправляется
письмо и не требуется подтверждение email, даже когда SMTP настроен.
В клиенте выберите вход, укажите `ADMIN` в поле «Email или ADMIN» и заданный пароль.

Создание выполняется только при отсутствии аккаунта `ADMIN` (без учёта регистра).
Повторный Deploy не меняет существующий пароль и данные администратора.
После создания аккаунта переменную можно удалить из Environment: аккаунт
сохранится в базе. Изменение переменной не является способом сброса пароля.
При пустой переменной новый администратор не создаётся.
Регистрация `ADMIN` через публичный API запрещена.

### SMTP через Stalwart

Для почтового аккаунта стенда добавьте в Environment, заменив примеры
SMTP-хоста, логина и отправителя своими значениями:

```dotenv
EMAIL_VERIFICATION_ENABLED=true
EMAIL_NOTIFICATIONS_ENABLED=true
SMTP_HOST=mail.example.com
SMTP_PORT=465
SMTP_USERNAME=hostapp@example.com
SMTP_FROM=hostapp@example.com
SMTP_AUTH=true
SMTP_SSL_ENABLED=true
SMTP_TLS_ENABLED=false
```

Отдельно задайте `SMTP_PASSWORD` через секреты Komodo или Environment.
Не используйте `*` как пароль: это обозначение скрытого значения.
В приложении используются `SMTP_USERNAME` и `SMTP_SSL_ENABLED`, а не
`SMTP_USER` и `SMTP_SECURE` из подсказки почтового сервера.
Порт 465 использует неявный SSL, поэтому STARTTLS (`SMTP_TLS_ENABLED`) отключён.
Проверка сертификата и имени SMTP-сервера включена. Адрес
SMTP-сервера должен разрешаться и быть доступен из контейнера backend.

Настроенный SMTP автоматически включает подтверждение email при регистрации.
Используйте обновлённый Desktop или CLI из той же ветки `javafx`: они показывают
ввод кода из письма. При изменении статуса заявки отправляется уведомление владельцу.
Для проверки зарегистрируйте новый аккаунт с доступным вам email и введите код.

Без SMTP оставьте его параметры пустыми и задайте `EMAIL_VERIFICATION_ENABLED=false`.
Полное описание почтовых параметров есть в основном README.

Пароль и имя пользователя PostgreSQL применяются при первом создании базы:
смена переменных не изменяет учётные данные в уже существующем volume.

## Nginx Proxy Manager

Создайте Proxy Host:

| Поле | Значение |
| --- | --- |
| Domain Names | Ваш домен стенда, например `hostapp.example.com` |
| Scheme | `http` |
| Forward Hostname / IP | `hostapp-staging-backend` |
| Forward Port | `8080` |

DNS домена должен вести на ваш Nginx Proxy Manager.
Настройте сертификат для домена на вкладке SSL и включите Force SSL.
HTTPS завершается на прокси, до backend запросы идут по HTTP внутри Docker.

Проверка: `https://hostapp.example.com/health` должен вернуть `OK`.
Swagger: `https://hostapp.example.com/swagger-ui.html`.

## Настольный клиент

На компьютере с клиентом из каталога `host-app-desktop`:

```sh
mvn -Dhostapp.api=https://hostapp.example.com javafx:run
```

Замените домен на свой. У API нет общего префикса `/api`.
Имя `hostapp-staging-backend` используется только внутри Docker-сети;
в клиенте задаётся домен, доступный с компьютера пользователя.

## Ручной запуск без Komodo

Из каталога `host-app-backend`:

```sh
cp .env.staging.example .env.staging
# Заполните POSTGRES_PASSWORD в .env.staging перед запуском.
docker compose --env-file .env.staging -f compose.staging.yaml up -d --build
```

Не добавляйте `.env.staging` с паролями в Git.
