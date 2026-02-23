# Этап сборки
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Копируем gradle файлы
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Копируем исходники фронтенда
COPY client client

# Собираем фронтенд (Kotlin JS)
RUN chmod +x gradlew
RUN ./gradlew :client:jsBrowserProductionWebpack --no-daemon

# Этап раздачи статики
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

# Очищаем дефолтную папку nginx
RUN rm -rf ./*

# Копируем собранные файлы фронтенда
# Путь в соответствии с client/build.gradle.kts: outputDirectory.set(file("$projectDir/build/dist/js/productionExecutable"))
COPY --from=build /app/client/build/dist/js/productionExecutable/ .

# Настройка Nginx для SPA (перенаправление всех запросов на index.html если нужно)
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
