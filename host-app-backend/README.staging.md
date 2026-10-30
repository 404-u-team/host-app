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
| Run directory | `host-app-backend` |
| File paths | Только `compose.staging.yaml` |
| Project name | `hostapp-staging` |
| Extra args | `--build` |

В Environment перенесите переменные из `.env.staging.example` и обязательно
задайте `POSTGRES_PASSWORD`. Реальные пароли храните в настройках стенда,
не в репозитории. Нажмите Deploy.

SMTP необязателен; параметры работают так же, как описано в основном README.
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
