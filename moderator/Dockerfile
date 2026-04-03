# stage: build
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app
ENV GRADLE_USER_HOME=/home/gradle/.gradle

COPY settings.gradle* build.gradle* build.gradle.kts* gradlew ./
COPY gradle ./gradle
RUN ["chmod", "+x", "./gradlew"]

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew --no-daemon build -x test -x detekt --parallel --quiet || true

COPY . .
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew --no-daemon bootJar -x test -x detekt --quiet

# stage: runtime
FROM eclipse-temurin:23-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
