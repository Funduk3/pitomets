package com.pitomets.monolit.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.RestAssured
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
abstract class BaseContainers {

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
    }

    companion object {
        val faker = Faker()
        val mapper = jacksonObjectMapper().apply { findAndRegisterModules() }

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine")
            .apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        }
    }
}