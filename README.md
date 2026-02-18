# MyWill Project

Проект состоит из серверной части на Spring Boot и клиентской части на Kotlin Multiplatform.

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

#### Shared Architecture (Compose Multiplatform)
To eliminate code duplication, both business logic and UI are implemented once in the `:client` module (`commonMain`) using **Compose Multiplatform**.

- **Shared UI**: The entire user interface (Auth, List, Editor) is written in Kotlin using Compose and shared between Android and Web.
- **AppController**: Encapsulates `ApiClient` calls, `AppState` management, and error handling.
- **Platform Shells**: Android and Web projects are now minimal "shells" that just initialize the platform-specific `ApiClient` and launch the shared `App` component.

This approach ensures 100% logic and UI reuse, identical behavior, and unified error messages across all platforms.

#### Сборка и запуск JS клиента (Web)
Для запуска в режиме разработки с автоматической перезагрузкой:
```bash
./gradlew :client:jsBrowserDevelopmentRun --continuous
```
Интерфейс будет доступен по адресу `http://localhost:8081`.

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

## Тестирование
Запуск всех тестов:
```bash
./gradlew test :client:jsTest
```
