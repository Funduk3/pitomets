package com.pitomets.monolit.integration

import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.LoginRequest
import com.pitomets.monolit.model.dto.request.RegisterRequest
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.model.dto.response.UserResponse
import io.restassured.RestAssured
import io.restassured.http.ContentType
import net.datafaker.Faker
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
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
class SellerProfileTest : BaseContainers() {

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        println("Testing on port: $port")
    }

    private fun registerUser(email: String, password: String): UserResponse {
        val registerReq = RegisterRequest(
            email = email,
            passwordHash = password,
            fullName = faker.name().fullName()
        )
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registerReq)
            .post("/register")
            .then()
            .statusCode(201)
            .extract()
            .`as`(UserResponse::class.java)
    }

    private fun login(email: String, password: String): TokenResponse {
        val loginReq = LoginRequest(email = email, passwordHash = password)
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReq)
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .`as`(TokenResponse::class.java)
    }

    @Test
    fun `should fail to update seller profile when profile does not exist`() {
        val email = faker.internet().emailAddress()
        val password = faker.internet().password(8, 16)

        registerUser(email, password)
        val tokens = login(email, password)

        // Попытка обновить несуществующий профиль
        val updateRequest = CreateSellerProfileRequest(
            shopName = "Non-existent shop",
            description = "Description"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .body(updateRequest)
            .put("/seller/profile")
            .then()
            .statusCode(
                Matchers.allOf(
                    Matchers.greaterThanOrEqualTo(400),
                    Matchers.lessThan(500)
                )
            )
    }

    @Test
    fun `should allow SELLER to update their profile`() {
        val email = faker.internet().emailAddress()
        val password = faker.internet().password(8, 16)

        registerUser(email, password)
        val tokens = login(email, password)

        // Создать профиль
        val originalShopName = faker.company().name()
        val createRequest = CreateSellerProfileRequest(
            shopName = originalShopName,
            description = "Original description"
        )

        val created = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .body(createRequest)
            .post("/seller/profile")
            .then()
            .statusCode(201)
            .extract()
            .`as`(SellerProfileResponse::class.java)

        // Перелогиниться для получения роли SELLER
        val sellerTokens = login(email, password)

        // Обновить профиль
        val updatedShopName = faker.company().name()
        val updateRequest = CreateSellerProfileRequest(
            shopName = updatedShopName,
            description = "Updated description"
        )

        val updated = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
            .body(updateRequest)
            .put("/seller/profile")
            .then()
            .statusCode(200)
            .extract()
            .`as`(SellerProfileResponse::class.java)

        // Проверки
        Assertions.assertEquals(created.id, updated.id)
        Assertions.assertEquals(updatedShopName, updated.shopName)
        Assertions.assertEquals("Updated description", updated.description)
        Assertions.assertNotEquals(originalShopName, updated.shopName)
    }

    @Test
    fun `should prevent creating duplicate seller profile`() {
        val email = faker.internet().emailAddress()
        val password = faker.internet().password(8, 16)

        registerUser(email, password)
        val tokens = login(email, password)

        val shopRequest = CreateSellerProfileRequest(
            shopName = faker.company().name(),
            description = "First shop"
        )

        // Первое создание - успех
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .body(shopRequest)
            .post("/seller/profile")
            .then()
            .statusCode(201)

        // Второе создание - должно упасть
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .body(shopRequest)
            .post("/seller/profile")
            .then()
            .statusCode(
                Matchers.allOf(
                    Matchers.greaterThanOrEqualTo(400),
                    Matchers.lessThan(501)
                )
            )
            .body("error", Matchers.notNullValue())
    }
}
