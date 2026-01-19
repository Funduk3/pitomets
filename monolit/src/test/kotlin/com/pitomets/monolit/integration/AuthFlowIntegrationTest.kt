package com.pitomets.monolit.integration

import com.fasterxml.jackson.module.kotlin.readValue
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.LoginRequest
import com.pitomets.monolit.model.dto.request.RefreshTokenRequest
import com.pitomets.monolit.model.dto.request.RegisterRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.testContainers.BaseContainers
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowIntegrationTest : BaseContainers() {

    @Test
    fun `full auth flow - register login refresh logout`() {
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
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThanOrEqualTo(500)))

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
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThanOrEqualTo(500)))

        // Неправильный пароль
        val loginReqBadPassword = LoginRequest(email = email, passwordHash = faker.internet().password(17, 18))
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReqBadPassword)
            .`when`()
            .post("/login")
            .then()
            .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThanOrEqualTo(500)))
    }

    @Test
    fun `should create seller profile when user has SELLER role`() {
        val username = faker.name().username()
        val password = faker.internet().password(8, 16)
        val email = faker.name().lastName() + "@mail.ru"

        val registerReq = RegisterRequest(email = email, passwordHash = password, fullName = username)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registerReq)
            .`when`()
            .post("/register")
            .then()
            .statusCode(201)

        val loginRequest = LoginRequest(email = email, passwordHash = password)
        val sellerTokens = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .`when`()
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .`as`(TokenResponse::class.java)

        val shopName = faker.funnyName().name()
        val createShopRequest = CreateSellerProfileRequest(shopName = shopName)
        val shopBody = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
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
        Assertions.assertEquals(shopName, response.shopName)
    }

    @Test
    fun `should upgrade user role from USER to SELLER after creating seller profile`() {
        val email = faker.internet().emailAddress()
        val password = faker.internet().password(8, 16)
        val fullName = faker.name().fullName()

        // 1. Регистрация
        val registerReq = RegisterRequest(email = email, passwordHash = password, fullName = fullName)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registerReq)
            .post("/register")
            .then()
            .statusCode(201)

        // 2. Первый логин (роль должна быть USER)
        val loginReq = LoginRequest(email = email, passwordHash = password)
        val userTokens = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReq)
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .`as`(TokenResponse::class.java)

        // buyer не может создать объявление
        val updateListingRequest = UpdateListingRequest(
            faker.funnyName().name(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", faker.number().randomDigit())
            .auth().oauth2(userTokens.accessToken)
            .body(updateListingRequest)
            .put("/listings/")
            .then()
            .statusCode(403)

        // 3. Создать профиль продавца
        val shopName = faker.company().name()
        val createShopRequest = CreateSellerProfileRequest(shopName = shopName, description = "Test shop")

        val profileResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(userTokens.accessToken)
            .body(createShopRequest)
            .post("/seller/profile")
            .then()
            .statusCode(201)
            .extract()
            .`as`(SellerProfileResponse::class.java)

        Assertions.assertNotNull(profileResponse.id)
        Assertions.assertEquals(shopName, profileResponse.shopName)

        // 4. Перелогиниться (роль должна стать SELLER)
        val sellerTokens = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReq)
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .`as`(TokenResponse::class.java)

        // 5. Проверить, что новый токен отличается от старого
        Assertions.assertNotEquals(userTokens.accessToken, sellerTokens.accessToken)

        // 6. Обновить профиль продавца с новым токеном (должно работать)
        val updateRequest = CreateSellerProfileRequest(
            shopName = "Updated Shop Name",
            description = "Updated description"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
            .body(updateRequest)
            .put("/seller/profile")
            .then()
            .statusCode(200)
    }

    @Test
    fun `should deny access to seller endpoints without authentication token`() {
        val createRequest = CreateSellerProfileRequest("Shop", "Description")

        // POST без токена
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .post("/seller/profile")
            .then()
            .statusCode(401)

        // PUT без токена
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .put("/seller/profile")
            .then()
            .statusCode(401)
    }
}
