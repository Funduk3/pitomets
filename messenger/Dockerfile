FROM gradle:8.14.3-jdk21 AS build

WORKDIR /app
ENV GRADLE_USER_HOME=/home/gradle/.gradle

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src
COPY config/detekt/detekt.yml /app/config/detekt/detekt.yml

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle build --no-daemon -x test -x detekt

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
