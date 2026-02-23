# MyWill Project

Проект состоит из серверной части на Spring Boot и клиентской части на Kotlin Multiplatform.

## LLM Context / Архитектура
Для быстрой ориентации LLM:
- **Backend**: Spring Boot 3, Kotlin, JPA (PostgreSQL), Liquibase, Spring Security (JWT + Google OAuth2).
  - `app`: Основной модуль бэкенда.
  - `entity/User`: Модель пользователя. Поля `is_dead` и `death_confirmed_at` управляют доступом к завещаниям.
  - `entity/TrustedPerson`: Список доверенных лиц. Если все доверенные ставят `confirmed_death = true`, включается таймер владельца.
  - `service/DeathCheckService`: Планировщик (@Scheduled), который переводит `is_dead` в true по истечении таймаута после подтверждения.
- **Frontend**: Kotlin Multiplatform (Compose Multiplatform), общий UI в модуле `client`.
  - `AppController`: Единая точка входа для UI, управляет `ApiClient` и `AppState`.
  - `AppState`: Реактивное состояние (Compose states) для всего приложения.

### Расширенный контекст для LLM (вся система целиком)

- Слои бэкенда (`:app`):
  - Входная точка: `org.mywill.server.App`.
  - Контроллеры (REST):
    - `org.mywill.server.controller.AuthController` — регистрация/логин/верификация; OAuth2 (Google).
    - `org.mywill.server.controller.ProfileController` — профиль пользователя: таймаут отмены смерти, "Я жив" и т.п.
    - `org.mywill.server.controller.WillController` — CRUD завещаний и доступ к их чтению.
    - `org.mywill.server.controller.TrustedPersonController` — управление доверенными лицами и подтверждением смерти.
  - Сервисы (бизнес-логика):
    - `org.mywill.server.service.AuthService` — логика регистрации, входа, работы с кодами/токенами.
    - `org.mywill.server.service.ProfileService` — операции с профилем и параметрами таймера.
    - `org.mywill.server.service.WillService` — создание/обновление/чтение завещаний и проверка прав доступа.
    - `org.mywill.server.service.TrustedPersonService` — добавление/удаление доверенных и подтверждение смерти.
    - `org.mywill.server.service.DeathCheckService` — планировщик, который по `deathConfirmedAt + timeout` проставляет
      `isDead = true`.
    - `org.mywill.server.service.EmailService` — отправка сервисных писем (приглашения/коды и т.п.).
  - Сущности (JPA):
    - `org.mywill.server.entity.User` — пользователь; ключевые поля: `isDead`, `deathConfirmedAt`.
    - `org.mywill.server.entity.Will` — завещание; содержит текст и список `allowedEmails`.
    - `org.mywill.server.entity.TrustedPerson` — доверенное лицо: `email`, `confirmedDeath` и ссылка на владельца.
  - Репозитории (Spring Data):
    - `UserRepository`, `WillRepository`, `TrustedPersonRepository` в пакете `org.mywill.server.repository`.
  - Безопасность и конфигурация:
    - `org.mywill.server.config.SecurityConfig` — конфигурация Spring Security (JWT + OAuth2).
    - `org.mywill.server.config.JwtAuthenticationFilter`, `org.mywill.server.config.JwtUtils` — извлечение/валидация
      JWT.
    - `org.mywill.server.config.ShedLockConfig` — конфигурация блокировок для планировщиков (если используется
      распределённая среда).
    - `org.mywill.server.config.converter.KotlinInstantConverter` — конвертер Kotlin `Instant` для Jackson.
    - Миграции БД — Liquibase (`app/src/main/resources/db/changelog`).
  - DTO и маппинг:
    - Пакет `org.mywill.server.controller.dto` содержит DTO (`AuthDto`, `ProfileDto`, `WillDto`, `TrustedPersonDto`).
    - Простейшие маппинги реализованы в сервисах/сущностях (см. `TrustedPerson.toDto()`).

- Потоки данных (главные кейсы):
  1) Авторизация через Google: `GET /oauth2/authorization/google` → коллбек → выдача JWT → фронт получает токен (см.
     `client/.../Main.kt`, обработка `#token=`) → все дальнейшие REST-запросы с `Authorization: Bearer <JWT>`.
  2) Подтверждение смерти: доверенные лица вызывают `POST /api/trusted-people/confirm-death` → при подтверждении всеми
     ставится `deathConfirmedAt` → `DeathCheckService` по таймеру проставляет `isDead=true` → открывается чтение
     завещаний для `allowedEmails`.
  3) Редактирование завещания: владелец вызывает `WillController` (`create/update`) → текст хранится в `Will` и доступен
     только владельцу до наступления `isDead=true`.

- Фронтенд (`:client`):
  - Входная точка Web: `org.mywill.client.Main` (Compose WASM/JS).
  - `org.mywill.client.AppController` — хранит токен, инкапсулирует вызовы `ApiClient`, управляет состоянием.
  - `org.mywill.client.ApiClient` — HTTP-клиент к бэкенду (авторизация, CRUD завещаний, доверенные лица, профиль).
  - UI-компоненты: `org.mywill.client.ui.*` — экраны Авторизации, Списков/Деталей, Редактора завещаний (Compose МР).

- Тесты:
  - Интеграционные тесты контроллеров/сервисов: `app/src/test/kotlin/org/mywill/server/*Test.kt` (см.
    `AuthControllerTest`, `WillControllerTest`, `DeathCheckServiceTest` и др.).

Подсказки для LLM при навигации:

- Если задача про REST/безопасность — смотрите `controller/*` и `config/*`.
- Если про бизнес-логику — `service/*` и сущности в `entity/*` + соответствующие тесты.
- Если про UI/клиента — `client/src/*/kotlin/org/mywill/client/*` (начиная с `Main.kt`, `AppController`, `ApiClient`).

## Требования
- JDK 25
- PostgreSQL 14+ (или Docker)

## Быстрый старт

### 1. Настройка конфигурации
Для запуска серверной части необходимо создать файл `app/src/main/resources/application-secret.yml`. 
Вы можете использовать следующий шаблон:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: postgres
  mail:
    username: your-email@gmail.com
    password: "your-app-specific-password"
jwt:
  secret: "your-very-secret-key-at-least-32-chars-long"
  expirationMs: 86400000
google:
  client-id: "your-google-client-id"
  client-secret: "your-google-client-secret"
```

> **Примечание:** Для Gmail используйте "Пароли приложений". Для Google OAuth2 необходимо создать Credentials в Google Cloud Console и добавить `http://localhost:8080/login/oauth2/code/google` в Authorized redirect URIs.

### 2. Запуск сервера (app)
Серверная часть реализована на Spring Boot. Для запуска выполните:

```bash
./gradlew :app:bootRun
```
Сервер будет доступен по адресу `http://localhost:8080`. База данных будет инициализирована автоматически через Liquibase.

### 3. Запуск клиента (client)
Клиентская часть реализована на Kotlin Multiplatform с общей бизнес-логикой в `commonMain`.

#### Общая архитектура (Compose Multiplatform)
Для исключения дублирования кода, бизнес-логика и пользовательский интерфейс реализованы один раз в модуле `:client` (`commonMain`) с использованием **Compose Multiplatform**.

- **Общий UI**: Весь интерфейс (Авторизация, Списки, Редактор) написан на Kotlin с использованием Compose и разделяется между Android и Web.
- **AppController**: Инкапсулирует вызовы `ApiClient`, управление состоянием `AppState` и обработку ошибок.
- **Платформенные оболочки**: Проекты для Android и Web теперь являются минимальными «оболочками», которые только инициализируют платформенный `ApiClient` и запускают общий компонент `App`.

Такой подход обеспечивает 100% переиспользование логики и UI, идентичное поведение и единые сообщения об ошибках на всех платформах.

#### Сборка и запуск JS клиента (Web)
Для запуска в режиме разработки с автоматической перезагрузкой:
```bash
./gradlew :client:jsBrowserDevelopmentRun --continuous
```
По умолчанию фронтенд открывается на `http://localhost:8081`. Для тестирования с внешних устройств (например, мобильного телефона в той же Wi‑Fi сети) рекомендуется запускать с указанием вашего локального IP (например, `192.168.1.8`):
```bash
./gradlew :client:jsBrowserDevelopmentRun
```
Убедитесь, что в `app/src/main/resources/application.yml` параметр `app.frontend-base-url` совпадает с адресом фронтенда (для `192.168.1.8` он уже настроен по умолчанию).

Для сборки production версии:
```bash
./gradlew :client:jsBrowserProductionWebpack
```
Результат будет в `client/build/dist/js/productionExecutable`.

#### Запуск JVM клиента (Desktop/Console)
```bash
./gradlew :client:run
```

#### Сборка для Android

Полноценное Android приложение, которое использует модуль `client`. 

> **Важно:** Для сборки Android приложения необходимо наличие Android SDK. Вы можете настроить его двумя способами:
> 1. Установите переменную окружения `ANDROID_HOME` указывающую на путь к Android SDK.
> 2. Создайте файл `local.properties` в корне проекта и добавьте строку `sdk.dir=путь_к_вашему_sdk` (например, `sdk.dir=C\:\\Users\\User\\AppData\\Local\\Android\\Sdk` на Windows).

Для сборки APK:
```bash
./gradlew :androidApp:assembleDebug
```
Результат будет в `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Функционал "Завещание" (Will)
1. **Регистрация**: Введите email и пароль.
2. **Верификация**: Проверьте почту (или логи сервера в режиме разработки) и введите код.
3. **Логин**: Авторизуйтесь для доступа к редактору.
4. **Редактор**: Напишите текст завещания и сохраните его.
5. **Доступ**: Укажите email людей, которые смогут просматривать ваше завещание.
### Бизнес-логика "Личный кабинет и Смерть"
1. Пользователь добавляет "Доверенных людей" по email.
2. Если все доверенные лица подтверждают факт смерти (через `/api/trusted-people/confirm-death`), у владельца проставляется `death_confirmed_at`.
3. Включается "окно отмены" (`death_timeout_seconds` в профиле). В течение этого времени владелец может нажать "Я жив" (`/api/profile/cancel-death`), что сбросит все подтверждения.
4. Если время вышло, `DeathCheckService` ставит `is_dead = true`.
5. Только после `is_dead = true` другие пользователи (указанные в `allowed_emails` завещания) могут прочитать текст завещания.

## Тестирование и покрытие кода (JaCoCo)
Запуск всех тестов проекта:
```bash
./gradlew test :client:jsTest
```

### Backend (модуль :app)
Для запуска тестов бэкенда и генерации отчета о покрытии:
```bash
./gradlew :app:test
```
После завершения в консоли будет выведена прямая ссылка на HTML-отчет, например:
`Jacoco HTML report: C:\...\app\build\reports\jacoco\test\html\index.html`

Для проверки соответствия порогам покрытия (настроены в `build.gradle.kts`):
```bash
./gradlew :app:jacocoTestCoverageVerification
```

## Развертывание (Docker & AWS App Runner)

Проект подготовлен для развертывания в **AWS App Runner** с использованием Docker-контейнеров.

### Команды для ручной сборки (из корня проекта)

**Сборка бэкенда:**

```bash
docker build -t my-will-backend -f app/Dockerfile .
```

**Сборка фронтенда (Web):**

```bash
docker build -t my-will-frontend -f client/Dockerfile .
```

### Настройка в AWS App Runner

* **Тип репозитория:** Container registry (ECR).
* **Контекст сборки (Build context):** Корень проекта (`.`).
* **Порты:**
    * **Бэкенд:** `8080` (настраивается через `SERVER_PORT`).
    * **Фронтенд:** `80`.
* **Маршрутизация фронтенда:** Nginx в контейнере настроен на поддержку SPA (все запросы перенаправляются на
  `index.html`).

#### Особенности настройки покрытия:
- **Исключения**: Из отчета исключены DTO, сущности, конфигурации и инфраструктурный код (Spring Security, Liquibase), чтобы сфокусироваться на проверке бизнес-логики.
- **Версия**: Используется JaCoCo 0.8.14 для полной поддержки Java 25.
- **Пороги**: На данный момент порог установлен в 0% для предотвращения сбоев сборки, но его рекомендуется постепенно повышать в `app/build.gradle.kts`.
