package com.pitomets.monolit.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pitomets.monolit.repository.UserRepo
import net.datafaker.Faker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Base64

@Suppress("UtilityClassWithPublicConstructor")
@Testcontainers
abstract class BaseContainers {

    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine")
            .apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }

        @JvmStatic
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply { withExposedPorts(6379) }

        init {
            postgres.start()
            redis.start()
        }

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

            val JWT_TEST_SECRET = Base64.getEncoder()
                .encodeToString("super-test-secret-key-which-is-long".toByteArray())
            registry.add("jwt.secret") { JWT_TEST_SECRET }
        }
        @LocalServerPort
        var port: Int = 0

        val faker = Faker()

        @Autowired
        lateinit var userRepo: UserRepo

        val mapper = jacksonObjectMapper().apply {
            findAndRegisterModules()
        }
    }
}
