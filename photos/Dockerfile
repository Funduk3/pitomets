FROM gradle:8.14.2-jdk25 AS build
WORKDIR /app
COPY . .
RUN gradle --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
