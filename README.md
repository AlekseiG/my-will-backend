# MyWill Project

Проект состоит из серверной части на Spring Boot и клиентской части на Kotlin Multiplatform.

## Сервер (app)
Серверная часть реализована на Spring Boot.
Эндпоинты:
- `GET /` - Приветствие.
- `GET /admin/ui` - Пример UI эндпоинта.

## Клиент (client)
Клиентская часть реализована как Kotlin Multiplatform проект с поддержкой JVM и JS.

### Запуск JVM клиента
Для запуска JVM клиента выполните команду:
```bash
./gradlew :client:run
```
(Требуется наличие работающего сервера на порту 8080)

### Сборка JS клиента
Для сборки JS версии:
```bash
./gradlew :client:jsBrowserProductionWebpack
```
Результат будет в `client/build/dist/js/productionExecutable`.
