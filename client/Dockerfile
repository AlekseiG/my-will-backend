# Этап сборки
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Копируем gradle файлы
COPY gradlew .
COPY gradle/ ./gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Копируем исходники фронтенда и необходимые модули для сборки всего проекта
COPY app app
COPY client client
COPY androidApp androidApp

# Собираем фронтенд (Kotlin JS)
RUN chmod +x gradlew
RUN ./gradlew :client:jsBrowserDistribution --no-daemon

# Проверка состава файлов после сборки в Gradle (для диагностики)
RUN ls -R client/build/dist || echo "dist not found"
RUN ls -R client/build/distributions || echo "distributions not found"

# Этап раздачи статики
FROM nginx:stable-alpine
# Устанавливаем envsubst (из пакета gettext) для подстановки переменных окружения в конфиг Nginx
RUN apk add --no-cache gettext
WORKDIR /usr/share/nginx/html

# Очищаем дефолтную папку nginx
RUN rm -rf ./*

# Копируем собранные файлы фронтенда.
# Пытаемся скопировать всё содержимое из папок, куда Compose/Kotlin JS обычно складывают результат
COPY --from=build /app/client/build/dist/ ./
COPY --from=build /app/client/build/distributions/ ./


# Отладочная команда для проверки состава файлов (используем ARG для сброса кэша)
ARG CACHEBUST=1
RUN ls -R /usr/share/nginx/html

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
