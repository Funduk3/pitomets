package com.pitomets.monolit.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.pitomets.monolit.model.dto.LoginRequest
import com.pitomets.monolit.model.dto.RefreshTokenRequest
import com.pitomets.monolit.model.dto.RegisterRequest
import com.pitomets.monolit.model.dto.TokenResponse
import io.restassured.RestAssured
import io.restassured.http.ContentType
import net.datafaker.Faker
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class AuthFlowIntegrationTest : BaseContainers() {

    @LocalServerPort
    var port: Int = 0

    private val mapper = jacksonObjectMapper()

    val faker = Faker()

    @Test
    fun `full auth flow - register login refresh logout`() {
        // Настройка RestAssured
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port

        val username = faker.name().username()
        val password = faker.internet().password(8, 16)

        // 1) Register
        val registerReq = RegisterRequest(fullName = username, passwordHash = password)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registerReq)
            .`when`()
            .post("/register")
            .then()
            .statusCode(201)

        // 2) Login
        val loginReq = LoginRequest(fullName = username, passwordHash = password)
        val loginBody = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReq)
            .`when`()
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .asString()

        Assertions.assertFalse(loginBody.isBlank(), "Login returned empty body")
        val tokens: TokenResponse = mapper.readValue(loginBody)
        Assertions.assertNotNull(tokens.accessToken)
        Assertions.assertNotNull(tokens.refreshToken)

        // 3) Refresh
        val refreshReq = RefreshTokenRequest(refreshToken = tokens.refreshToken)
        val refreshBody = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(refreshReq)
            .`when`()
            .post("/refresh")
            .then()
            .statusCode(200)
            .extract()
            .asString()

        val newTokens: TokenResponse = mapper.readValue(refreshBody)
        Assertions.assertNotNull(newTokens.accessToken)
        Assertions.assertNotNull(newTokens.refreshToken)

        // 4) Logout
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(newTokens.accessToken)
            .body(RefreshTokenRequest(newTokens.refreshToken))
            .`when`()
            .post("/logout")
            .then()
            .statusCode(200)

        // 4a) Попытка logout с повреждённым access token должна вернуть 4xx (тест токена)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(newTokens.accessToken + "tampered") // подменяем токен
            .body(RefreshTokenRequest(newTokens.refreshToken))
            .`when`()
            .post("/logout")
            .then()
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThan(500)))

        // 4b) Попытка logout без access token должна вернуть 4xx (тест токена)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(RefreshTokenRequest(newTokens.refreshToken))
            .`when`()
            .post("/logout")
            .then()
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThan(500)))

        // 5) Попытка повторного refresh должна возвращать 4xx
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(RefreshTokenRequest(newTokens.refreshToken))
            .`when`()
            .post("/refresh")
            .then()
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThan(500)))

        // Вымышленный username
        val usernameIncorrect = faker.name().username()
        val loginReqBadUsername = LoginRequest(fullName = usernameIncorrect, passwordHash = faker.internet().password(8, 16))
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReqBadUsername)
            .`when`()
            .post("/login")
            .then()
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThan(500)))

        // Неправильный пароль
        val loginReqBadPassword = LoginRequest(fullName = username, passwordHash = faker.internet().password(17,18))
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReqBadPassword)
            .`when`()
            .post("/login")
            .then()
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThan(500)))
    }
}
