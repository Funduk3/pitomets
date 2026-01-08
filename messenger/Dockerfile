# Используем официальный образ для Java
FROM openjdk:21-jdk-slim as build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файлы конфигурации Gradle
COPY gradle /app/gradle
COPY build.gradle /app/build.gradle
COPY settings.gradle /app/settings.gradle
COPY gradlew /app/

# Копируем исходный код проекта
COPY src /app/src

# Устанавливаем зависимости и собираем приложение
RUN ./gradlew build --no-daemon

# Используем более легкий образ для запуска
FROM openjdk:21-jre-slim

WORKDIR /app

# Копируем собранный JAR файл из предыдущего шага
COPY --from=build /app/build/libs/messenger-1.0-SNAPSHOT.jar /app/messenger.jar

# Открываем порт для приложения
EXPOSE 8080

# Запускаем приложение
CMD ["java", "-jar", "/app/messenger.jar"]
