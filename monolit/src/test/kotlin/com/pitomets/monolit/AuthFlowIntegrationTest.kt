package com.pitomets.monolit.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.pitomets.monolit.model.dto.LoginRequest
import com.pitomets.monolit.model.dto.RegisterRequest
import com.pitomets.monolit.model.dto.RefreshTokenRequest
import com.pitomets.monolit.model.dto.TokenResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import net.datafaker.Faker

import java.util.Base64

/**
 * Интеграционный тест регистрации/логина/рефреша/логаута.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFlowIntegrationTest {
    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine")
            .apply {
                withDatabaseName("testdb");
                withUsername("test");
                withPassword("test")
            }

        @JvmStatic
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply { withExposedPorts(6379) }

        init {
            postgres.start()
            redis.start()
            Runtime.getRuntime().addShutdownHook(Thread {
                try {
                    postgres.stop()
                } catch (_: Throwable) {
                }
                try {
                    redis.stop()
                } catch (_: Throwable) {
                }
            })
        }

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }

            // <- ВНИМАНИЕ: используем имя, которое твоё приложение читает:
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort.toString() }
            val JWT_TEST_SECRET =
                Base64.getEncoder().encodeToString("super-test-secret-key-which-is-long".toByteArray())
            registry.add("jwt.secret") { JWT_TEST_SECRET }
        }
    }

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var rest: TestRestTemplate

    @Test
    fun `full auth flow - register login refresh logout`() {
        val base = "http://localhost:$port"

        val faker = Faker() // <- создаем Faker

        // Генерируем случайные данные для пользователя
        val randomName = faker.name().username()  // имя пользователя
        val randomPassword = faker.internet().password(8, 16) // случайный пароль длиной 8-16 символов

        // 1) Register
        val registerReq = RegisterRequest(fullName = randomName, passwordHash = randomPassword)
        val registerResp = rest.postForEntity("$base/register", HttpEntity(registerReq), Any::class.java)
        assertEquals(HttpStatus.CREATED, registerResp.statusCode)

        // 2) Login
        val loginReq = LoginRequest(fullName = randomName, passwordHash = randomPassword)
        val loginStringResp = rest.postForEntity("$base/login", HttpEntity(loginReq), String::class.java)
        println("LOGIN status=${loginStringResp.statusCode}, body=${loginStringResp.body}")
        assertEquals(HttpStatus.OK, loginStringResp.statusCode, "Login should return 200 OK")

        val mapper = jacksonObjectMapper()

        val tokens: TokenResponse = try {
            if (loginStringResp.body.isNullOrBlank()) {
                throw AssertionError("Login returned empty body; check server logs")
            }
            mapper.readValue(loginStringResp.body!!)
        } catch (ex: Exception) {
            println("Failed to parse login response JSON: ${ex.message}")
            throw ex
        }

        assertNotNull(tokens.accessToken, "accessToken must be present")
        assertNotNull(tokens.refreshToken, "refreshToken must be present")

        // 3) Refresh (use refresh token to get new tokens)
        val refreshReq = RefreshTokenRequest(refreshToken = tokens.refreshToken)
        val refreshStringResp = rest.postForEntity("$base/refresh", HttpEntity(refreshReq), String::class.java)
        println("REFRESH status=${refreshStringResp.statusCode}, body=${refreshStringResp.body}")

        assertEquals(HttpStatus.OK, refreshStringResp.statusCode, "Refresh should return 200 OK")

        if (refreshStringResp.body.isNullOrBlank()) {
            throw AssertionError("Refresh returned empty body. Server likely failed to generate tokens. Check server logs above.")
        }
        val newTokens: TokenResponse = try {
            mapper.readValue(refreshStringResp.body!!)
        } catch (ex: Exception) {
            println("Failed to parse refresh response JSON: ${ex.message}")
            throw ex
        }

        assertNotNull(newTokens.accessToken)
        assertNotNull(newTokens.refreshToken)

        // 4) Logout (invalidate refresh token) refresh token в body, а access в хедерах
        val headers = HttpHeaders().apply {
            setBearerAuth(newTokens.accessToken)
        }
        val logoutReq = RefreshTokenRequest(newTokens.refreshToken)

        val logoutResp = rest.postForEntity("$base/logout", HttpEntity(logoutReq, headers), String::class.java)
        println("LOGOUT status=${logoutResp.statusCode}, body=${logoutResp.body}")
        assertEquals(HttpStatus.OK, logoutResp.statusCode)


        // 5) Trying to refresh again with same token should fail (token deleted)
        val secondRefreshResp: ResponseEntity<String> =
            rest.postForEntity(
                "$base/refresh",
                HttpEntity(RefreshTokenRequest(newTokens.refreshToken)),
                String::class.java
            )
        // Ожидаем 4xx
        assertTrue(secondRefreshResp.statusCode.is4xxClientError)
    }
}