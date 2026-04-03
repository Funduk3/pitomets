plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("io.ktor.plugin") version "3.4.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    application // добавляем этот plugin
}

group = "com.pitomets"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:3.4.2")
    implementation("io.ktor:ktor-server-netty:3.4.2")
    implementation("io.ktor:ktor-server-websockets:3.4.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.2")
    implementation("io.ktor:ktor-server-status-pages:3.4.2")
    implementation("io.ktor:ktor-server-call-logging:3.4.2")
    implementation("io.ktor:ktor-server-cors:3.4.2")

    // PostgreSQL & Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.48.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.48.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.48.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.48.0")
    implementation("org.postgresql:postgresql:42.7.3")

    // Ktor Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.24")

    // HikariCP for connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Tests
    testImplementation("io.ktor:ktor-server-tests:3.4.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.0.21")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

application {
    mainClass.set("com.pitomets.messenger.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("messenger.jar")
    }
}

detekt {
    toolVersion = "1.23.8"
    parallel = true
    buildUponDefaultConfig = true
    allRules = false
    config = files("config/detekt/detekt.yml")
    baseline = file("config/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    autoCorrect = true

    reports {
        html.required.set(true)
        html.outputLocation.set(file("build/reports/detekt.html"))
        xml.required.set(true)
        xml.outputLocation.set(file("build/reports/detekt.xml"))
        txt.required.set(true)
        txt.outputLocation.set(file("build/reports/detekt.txt"))
    }
}

// Запуск detekt перед сборкой
tasks.named("compileKotlin") {
    dependsOn("detekt")
}

// Альтернативно: запуск detekt перед тестами
tasks.named("test") {
    dependsOn("detekt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Или запуск перед созданием jar
tasks.named("jar") {
    dependsOn("detekt")
}
