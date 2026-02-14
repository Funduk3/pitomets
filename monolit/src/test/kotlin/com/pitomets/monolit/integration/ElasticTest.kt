package com.pitomets.monolit.integration

import com.pitomets.monolit.model.Gender
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.response.SearchListingsPageResponse
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import com.pitomets.monolit.testContainers.BaseContainers
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import kotlin.test.assertEquals

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class ElasticTest : BaseContainers() {

    @Test
    fun `Create a lot of listings and search should return most similar`() {
        val tokenSeller = createBaseSeller()

        // уникальный токен, по которому будем искать
        val token = "unique-search-token-${System.currentTimeMillis()}"

        createSomeListings(8, tokenSeller)

        // Создаём 2 целевых объявления, где токен в title
        val targetTitle1 = "Best match A $token"
        val targetTitle2 = "Best match B $token"

        val targetReq1 = ListingsRequest(
            description = "This is the best match A for $token",
            species = faker.name().name(),
            ageMonths = faker.number().numberBetween(1, 3),
            price = BigDecimal.valueOf(faker.number().numberBetween(1L, 24L)),
            breed = null,
            title = targetTitle1,
            cityId = 4L,
            metroId = null,
            gender = Gender.M,
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokenSeller.accessToken)
            .body(targetReq1)
            .post("/listings/")
            .then()
            .statusCode(200)

        val targetReq2 = ListingsRequest(
            description = "This is the best match B for $token",
            species = faker.name().name(),
            ageMonths = faker.number().numberBetween(1, 3),
            price = BigDecimal.valueOf(faker.number().numberBetween(1L, 24L)),
            breed = null,
            title = targetTitle2,
            cityId = 4L,
            metroId = null,
            gender = Gender.M,
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokenSeller.accessToken)
            .body(targetReq2)
            .post("/listings/")
            .then()
            .statusCode(200)

        listingOutboxProcessor.processOutbox()
        elasticClient.indices().refresh { r -> r.index("listings") }

        // Убедимся, что два результата содержат наш токен в title
        val page: SearchListingsPageResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .param("query", token, "size", 2)
            .get("/search/listings")
            .then()
            .statusCode(200)
            .extract()
            .`as`(SearchListingsPageResponse::class.java)
        val list = page.items

        Assertions.assertTrue(
            list.size >= 2,
            "Expected at least 2 search results, but got ${list.size}"
        )
        list.forEach { dto ->
            Assertions.assertNotNull(dto.id, "id should not be null")
            Assertions.assertNotNull(dto.title, "title should not be null")
            Assertions.assertNotNull(dto.description, "description should not be null")
        }
    }

    @Test
    fun `should create listing and fetch similar listings`() {
        val sellerTokens = createBaseSeller()

        val token = "SIMILAR-${System.currentTimeMillis()}"

        val createListingRequest = ListingsRequest(
            description = "Base desc $token",
            species = faker.funnyName().name(),
            ageMonths = faker.number().numberBetween(1, 3),
            price = BigDecimal(faker.number().randomDigit()),
            breed = null,
            title = "Base title $token",
            cityId = 4L,
            metroId = null,
            gender = Gender.M,
        )

        val similarListingRequest = ListingsRequest(
            description = "Similar desc $token",
            species = faker.funnyName().name(),
            ageMonths = faker.number().numberBetween(1, 3),
            price = BigDecimal(faker.number().randomDigit()),
            breed = null,
            title = "Similar title $token",
            cityId = 4L,
            metroId = null,
            gender = Gender.M,
        )

        createSomeListings(8, sellerTokens)

        val createdListingId: Long = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path("listingsId")

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
            .body(similarListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)

        // Обрабатываем Outbox для индексации в Elasticsearch
        listingOutboxProcessor.processOutbox()
        elasticClient.indices().refresh { it.index("listings") }

        // Ищем похожие
        val similarListings: List<SearchListingsResponse> = RestAssured.given()
            .contentType(ContentType.JSON)
            .param("size", 3)
            .get("/search/listings/$createdListingId/similar")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<SearchListingsResponse>::class.java)
            .toList()

        assertEquals(1, similarListings.size)
    }
}
