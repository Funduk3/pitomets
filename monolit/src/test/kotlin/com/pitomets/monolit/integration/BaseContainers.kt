package com.pitomets.monolit.integration

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.support.TestPropertySourceUtils
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.lifecycle.Startables
import java.util.Base64

@Testcontainers
@ContextConfiguration(initializers = [BaseContainers.Companion.Initializer::class])
class BaseContainers {

    companion object {
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
        val redis = GenericContainer<Nothing>("redis:7-alpine")
            .apply { withExposedPorts(6379) }

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }

            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort.toString() }

            val jwt = Base64.getEncoder()
                .encodeToString("super-test-secret-key-which-is-long".toByteArray())
            registry.add("jwt.secret") { jwt }
        }

        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(context: ConfigurableApplicationContext) {
                Startables.deepStart(java.util.stream.Stream.of(postgres, redis)).join()


                val jwt = Base64.getEncoder()
                    .encodeToString("super-test-secret-key-which-is-long".toByteArray())

                val registry = TestPropertyValues.of(
                    "spring.datasource.url=${postgres.jdbcUrl}",
                    "spring.datasource.username=${postgres.username}",
                    "spring.datasource.password=${postgres.password}",
                    "spring.data.redis.host=${redis.host}",
                    "spring.data.redis.port=${redis.firstMappedPort}",

                    "jwt.secret=$jwt"
                )

                registry.applyTo(context.environment)
            }
        }
    }
}
