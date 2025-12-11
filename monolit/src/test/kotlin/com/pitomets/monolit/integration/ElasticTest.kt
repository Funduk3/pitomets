package com.pitomets.monolit.integration

import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.service.SearchService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import net.datafaker.Faker
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class ElasticTest : BaseContainers() {

    @LocalServerPort
    var port: Int = 0

    val faker = Faker()

    @Autowired
    lateinit var userRepo: UserRepo

    @Autowired
    lateinit var petsRepo: PetsRepo

    @Autowired
    lateinit var listingsRepo: ListingsRepo

    @Autowired
    lateinit var searchService: SearchService

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        println("Testing on port: $port")
    }

    @Test
    fun `Create a lot of listings and search should return most similar`() {
        // todo потом все тесты сделать красивыми
        val email = faker.internet().emailAddress()
        val password = faker.internet().password(8, 16)
        registerUser(email, password)
        val tokens = login(email, password)
        // Создать профиль
        val createRequest = CreateSellerProfileRequest(
            shopName = faker.company().name(),
            description = "Original description"
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .body(createRequest)
            .post("/seller/profile")
            .then()
            .statusCode(201)
        // Перелогиниться для роли SELLER
        val sellerTokens = login(email, password)
        val updateRequest = CreateSellerProfileRequest(
            shopName = faker.company().name(),
            description = "Updated description"
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
            .body(updateRequest)
            .put("/seller/profile")
            .then()
            .statusCode(200)

        // уникальный токен, по которому будем искать
        val token = "unique-search-token-${System.currentTimeMillis()}"

        // Создаём 8 шумовых объявлений
        repeat(8) {
            val req = ListingsRequest(
                description = faker.lorem().sentence(),
                species = faker.animal().name(),
                ageMonths = faker.number().numberBetween(1, 24),
                price = BigDecimal.valueOf(faker.number().numberBetween(1, 100).toLong()),
                breed = null,
                title = faker.book().title()
            )
            RestAssured.given()
                .contentType(ContentType.JSON)
                .auth().oauth2(sellerTokens.accessToken)
                .body(req)
                .post("/listings/")
                .then()
                .statusCode(200)
        }

        // Создаём 2 целевых объявления, где токен в title
        val targetTitle1 = "Best match A $token"
        val targetTitle2 = "Best match B $token"

        val targetReq1 = ListingsRequest(
            description = "This is the best match A for $token",
            species = faker.name().name(),
            ageMonths = faker.number().numberBetween(1, 24),
            price = BigDecimal.valueOf(faker.number().numberBetween(1L, 24L)),
            breed = null,
            title = targetTitle1
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
            .body(targetReq1)
            .post("/listings/")
            .then()
            .statusCode(200)

        val targetReq2 = ListingsRequest(
            description = "This is the best match B for $token",
            species = faker.name().name(),
            ageMonths = faker.number().numberBetween(1, 24),
            price = BigDecimal.valueOf(faker.number().numberBetween(1L, 24L)),
            breed = null,
            title = targetTitle2
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
            .body(targetReq2)
            .post("/listings/")
            .then()
            .statusCode(200)

        val searchBody = mapOf("query" to token, "page" to 0, "size" to 2)

        // Убедимся, что два результата содержат наш токен в title
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(searchBody)
            .post("/search/listings")
            .then()
            .statusCode(200)
            .body("size()", Matchers.equalTo(2))
            .body(
                "[0].title",
                Matchers.anyOf(
                    Matchers.containsString(token),
                    Matchers.equalTo(targetTitle1),
                    Matchers.equalTo(targetTitle2)
                )
            )
            .body(
                "[1].title",
                Matchers.anyOf(
                    Matchers.containsString(token),
                    Matchers.equalTo(targetTitle1),
                    Matchers.equalTo(targetTitle2)
                )
            )
    }
}
