plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.2.21"
}

group = "com.pitomets"
version = "0.0.1-SNAPSHOT"
description = "notifications"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    testImplementation("io.rest-assured:rest-assured:5.5.6")
    testImplementation("io.rest-assured:kotlin-extensions:5.5.6")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Jackson 2.x (Spring Boot 4.x совместимость)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("com.h2database:h2")
    testImplementation("net.datafaker:datafaker:1.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testImplementation("org.testcontainers:testcontainers:1.20.1")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1") // <-- ЭТА ЗАВИСИМОСТЬ ОБЯЗАТЕЛЬНА
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.testcontainers:kafka:1.20.1")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.1")


}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    environment("DOCKER_HOST", "tcp://localhost:2375")
    environment("TESTCONTAINERS_RYUK_DISABLED", "true") // Optional: speeds up tests
}