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
# Устанавливаем envsubst (из пакета gettext) для подстановки переменных окружения в конфиг Nginx
RUN apk add --no-cache gettext
WORKDIR /usr/share/nginx/html

# Очищаем дефолтную папку nginx
RUN rm -rf ./*

# Копируем собранные файлы фронтенда
# Путь в соответствии с client/build.gradle.kts: outputDirectory.set(file("$projectDir/build/dist/js/productionExecutable"))
COPY --from=build /app/client/build/dist/js/productionExecutable/ .

# Настройка Nginx для SPA и поддержка динамического порта через Railway
RUN echo 'server { \
    listen ${PORT}; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf.template

ENV PORT=80

CMD ["sh", "-c", "envsubst '${PORT}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"]
