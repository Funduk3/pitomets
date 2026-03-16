# stage: build
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Копируем только файлы, нужные для разрешения зависимостей — это ускоряет кеширование Docker-слоёв
COPY settings.gradle* build.gradle* build.gradle.kts* gradlew ./
COPY gradle ./gradle
# Сделаем скрипт gradlew исполняемым
RUN ["chmod", "+x", "./gradlew"]

# Запускаем зависимостную фазу (без сборки приложения)
# Первый вызов скачает зависимости и закэширует их в слое
RUN ./gradlew --no-daemon build -x test -x detekt --parallel --quiet || true

# Теперь копируем весь проект и соберём bootJar
COPY . .
RUN ./gradlew --no-daemon bootJar -x test -x detekt --quiet

# stage: runtime
FROM eclipse-temurin:25-jre
WORKDIR /app

# Install curl for healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Копируем jar, который создал Spring Boot (bootJar)
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# Необязательно: задать JVM-параметры через переменные окружения
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
