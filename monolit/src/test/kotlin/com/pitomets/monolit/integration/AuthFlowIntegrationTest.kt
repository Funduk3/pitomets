package com.pitomets.monolit.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.LoginRequest
import com.pitomets.monolit.model.dto.request.RefreshTokenRequest
import com.pitomets.monolit.model.dto.request.RegisterRequest
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.model.dto.response.TokenResponse
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

    private val mapper = jacksonObjectMapper().apply {
        findAndRegisterModules()
    }
    val faker = Faker()

    @Test
    fun `full auth flow - register login refresh logout`() {
        // Настройка RestAssured
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port

        val username = faker.name().username()
        val password = faker.internet().password(8, 16)
        val email = faker.name().lastName() + "@mail.ru"

        // 1) Register
        val registerReq = RegisterRequest(email = email, passwordHash = password, fullName = username)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registerReq)
            .`when`()
            .post("/register")
            .then()
            .statusCode(201)

        // 2) Login
        val loginReq = LoginRequest(email = email, passwordHash = password)
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

        // Register seller
        val shopName = faker.funnyName().name()
        val createShopRequest = CreateSellerProfileRequest(shopName = shopName)
        val shopBody = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .body(createShopRequest)
            .`when`()
            .post("/seller/profile")
            .then()
            .statusCode(201)
            .extract()
            .asString()

        Assertions.assertFalse(shopBody.isBlank())
        val response: SellerProfileResponse = mapper.readValue(shopBody)
        Assertions.assertNotNull(response.id)
        Assertions.assertNotNull(response.shopName)

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
        val emailIncorrect = faker.name().username() + "@mail.ru"
        val loginReqBadUsername = LoginRequest(
            email = emailIncorrect,
            passwordHash = faker.internet().password(8, 16)
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReqBadUsername)
            .`when`()
            .post("/login")
            .then()
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThan(500)))

        // Неправильный пароль
        val loginReqBadPassword = LoginRequest(email = email, passwordHash = faker.internet().password(17, 18))
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReqBadPassword)
            .`when`()
            .post("/login")
            .then()
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThan(500)))
    }
}
