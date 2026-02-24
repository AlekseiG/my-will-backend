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
# Устанавливаем envsubst (из пакета gettext) для подстановки переменных окружения в конфиг Nginx
RUN apk add --no-cache gettext
WORKDIR /usr/share/nginx/html

# Очищаем дефолтную папку nginx и удаляем стандартный конфиг
RUN rm -rf /usr/share/nginx/html/* && rm -rf /etc/nginx/conf.d/*

# Копируем собранные файлы фронтенда.
# Пытаемся скопировать всё содержимое из папок, куда Compose/Kotlin JS обычно складывают результат
COPY --from=build /app/client/build/dist/ ./


# Отладочная команда для проверки состава файлов (используем ARG для сброса кэша)
ARG CACHEBUST=1
RUN ls -R /usr/share/nginx/html

# Настройка Nginx для SPA и поддержка динамического порта через Railway
RUN echo 'server { \
    listen ${PORT} default_server; \
    server_name _; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf.template

# Перезаписываем основной конфиг nginx, чтобы он ТОЧНО не использовал дефолты
# Используем одинарные кавычки для EOF, чтобы переменные $ не раскрывались оболочкой при записи
RUN printf "user  nginx;\n\
worker_processes  auto;\n\
error_log  /var/log/nginx/error.log notice;\n\
pid        /var/run/nginx.pid;\n\
events {\n\
    worker_connections  1024;\n\
}\n\
http {\n\
    include       /etc/nginx/mime.types;\n\
    default_type  application/octet-stream;\n\
    log_format  main  '\$remote_addr - \$remote_user [\$time_local] \"\$request\" '\n\
                      '\$status \$body_bytes_sent \"\$http_referer\" '\n\
                      '\"\$http_user_agent\" \"\$http_x_forwarded_for\"';\n\
    access_log  /var/log/nginx/access.log  main;\n\
    sendfile        on;\n\
    keepalive_timeout  65;\n\
    include /etc/nginx/conf.d/*.conf;\n\
}\n" > /etc/nginx/nginx.conf

ENV PORT=80

CMD ["sh", "-c", "envsubst '${PORT}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && cat /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"]
