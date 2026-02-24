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

# Этап раздачи статики
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

# Очищаем всё
RUN rm -rf ./* /etc/nginx/conf.d/* /etc/nginx/nginx.conf

# Копируем фронтенд
COPY --from=build /app/client/build/dist/ ./

# Проверочный файл
RUN echo "Frontend is active" > test.txt

# Максимально простой конфиг в корневом nginx.conf
RUN echo 'events { worker_connections 1024; } \
http { \
    include /etc/nginx/mime.types; \
    server { \
        listen __PORT__ default_server; \
        root /usr/share/nginx/html; \
        index index.html; \
        location / { \
            try_files $uri $uri/ /index.html; \
        } \
    } \
}' > /etc/nginx/nginx.conf.template

ENV PORT=8080
EXPOSE ${PORT}

CMD ["sh", "-c", "sed \"s/__PORT__/${PORT}/g\" /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf && exec nginx -g \"daemon off;\""]
