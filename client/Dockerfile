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

# Этап раздачи статики
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

# Очищаем всё
RUN rm -rf ./* /etc/nginx/conf.d/* /etc/nginx/nginx.conf

# Копируем фронтенд
# 1) Явно копируем productionExecutable в корень (там у нас index.html и client.js)
COPY --from=build /app/client/build/dist/js/productionExecutable/ ./
# 2) Дополнительно копируем остальной dist (если там есть ассеты)
COPY --from=build /app/client/build/dist/ ./

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
ENV BACKEND_URL=""
EXPOSE ${PORT}

CMD ["sh", "-c", "sed -i \"s|__BACKEND_URL__|${BACKEND_URL}|g\" /usr/share/nginx/html/index.html && sed \"s/__PORT__/${PORT}/g\" /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf && exec nginx -g \"daemon off;\""]
