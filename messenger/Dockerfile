FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

COPY gradlew ./gradlew
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src
COPY config/detekt/detekt.yml /app/config/detekt/detekt.yml

RUN chmod +x ./gradlew
RUN ./gradlew build --no-daemon -x test -x detekt

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
