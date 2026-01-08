plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.0"  // Если используется Ktor
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0"
}

group = "com.messenger"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()  // Репозиторий для зависимостей
    google()  // Можно добавить Google, если нужно
}
dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-websockets:2.3.0")

    // PostgreSQL
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.postgresql:postgresql:42.5.0")

    // Ktor Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")

    // Логирование
    implementation("io.ktor:ktor-server-call-logging:2.3.0")

    // Тесты
    testImplementation("io.ktor:ktor-server-tests:2.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0")
}

kotlin {
    jvmToolchain(21)  // Указываем использование JVM 21
}
