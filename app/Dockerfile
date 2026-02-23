# Этап сборки
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Копируем gradle файлы для кеширования зависимостей
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Копируем исходники модулей, необходимых для сборки бэкенда
COPY app app

# Даем права на выполнение gradle и собираем бэкенд
RUN chmod +x gradlew
RUN ./gradlew :app:bootJar --no-daemon

# Этап запуска
FROM eclipse-temurin:25-jre
WORKDIR /app

# Копируем собранный jar из этапа сборки
COPY --from=build /app/app/build/libs/*.jar app.jar

# AWS App Runner по умолчанию слушает порт 8080, но это настраиваемо
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
