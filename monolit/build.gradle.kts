plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.0.21"
    id("org.openapi.generator") version "7.17.0"

    // Detekt
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "com.pitomets"
version = "0.0.1-SNAPSHOT"
description = "monolit"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21) // todo 25 (kotlin 2.3)
	}
}

repositories {
	mavenCentral()
}

dependencies {
    // spring
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.10")

	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("redis.clients:jedis")
    implementation("org.springframework.boot:spring-boot-starter-validation")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.swagger:swagger-annotations:1.6.6")

    // testcontainers
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("net.datafaker:datafaker:1.4.0")
    testImplementation("io.rest-assured:rest-assured:5.5.6")
    testImplementation("io.rest-assured:kotlin-extensions:5.5.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

    // detekt
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

sourceSets {
    main {
        kotlin.srcDir("$buildDir/generated/openapi/src/main/kotlin")
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

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/src/main/resources/openapi.yaml")
    outputDir.set("$buildDir/generated/openapi")

    apiPackage.set("com.pitomets.monolit.api")
    modelPackage.set("com.pitomets.monolit.model.dto")
    invokerPackage.set("com.pitomets.monolit.invoker")

    // опции (для Spring Boot 3 / jakarta)
    configOptions.set(
        mapOf(
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "useSwaggerAnnotations" to "false",
            "serializationLibrary" to "jackson",
            "dateLibrary" to "java8",
            "interfaceOnly" to "true"
        )
    )
    // Если ваша версия плагина требует additionalProperties:
    additionalProperties.set(mapOf("useSpringBoot3" to "true"))
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
    dependsOn("detekt")
}

springBoot {
    mainClass.set("com.pitomets.monolit.MonolitApplicationKt")
}

detekt {
    toolVersion = "1.23.8"
    parallel = true
    buildUponDefaultConfig = true
    allRules = false
    config = files("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    this.autoCorrect = true

    reports {
        html.required.set(true)
        html.outputLocation.set(file("build/reports/detekt.html"))
        xml.required.set(true)
        xml.outputLocation.set(file("build/reports/detekt.xml"))
        txt.required.set(true)
        txt.outputLocation.set(file("build/reports/detekt.txt"))
    }
}

tasks.named("bootRun") {
    dependsOn("detekt")
}

