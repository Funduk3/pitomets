package com.pitomets.monolit.integration

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.util.Base64

@Testcontainers
abstract class BaseContainers {

    companion object {
        @Container
        @JvmField
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15-alpine")
            .apply {
                withDatabaseName("monolit")
                withUsername("postgres")
                withPassword("postgres")
                withStartupTimeout(Duration.ofSeconds(90))
                waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(90)))
            }

        @Container
        @JvmField
        val redis: GenericContainer<*> = GenericContainer("redis:7-alpine")
            .apply {
                withExposedPorts(6379)
                withCommand("redis-server", "--save", "\"\"", "--appendonly", "no")
                withStartupTimeout(Duration.ofSeconds(120))
                waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(120)))
            }
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            if (!postgres.isRunning) postgres.start()
            if (!redis.isRunning) redis.start()

            // Postgres
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }

            // Redis — убедитесь, что ваше приложение читает эти свойства (spring.redis.host/port)
            registry.add("spring.redis.host") { redis.host }
            registry.add("spring.redis.port") { redis.firstMappedPort.toString() }

            // Другие тестовые проперти
            val jwtSecret = Base64.getEncoder()
                .encodeToString("super-test-secret-key-which-is-long".toByteArray())
            registry.add("jwt.secret") { jwtSecret }
        }
    }
}
