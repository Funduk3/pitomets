package com.pitomets.monolit.integration

import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal

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
                .auth().oauth2(tokenSeller.accessToken)
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
            .auth().oauth2(tokenSeller.accessToken)
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
            .auth().oauth2(tokenSeller.accessToken)
            .body(targetReq2)
            .post("/listings/")
            .then()
            .statusCode(200)

        listingOutboxProcessor.processOutbox()
        elasticClient.indices().refresh { r -> r.index("listings") }

        // Убедимся, что два результата содержат наш токен в title
        val list: List<SearchListingsResponse> = RestAssured.given()
            .contentType(ContentType.JSON)
            .param("query", token, "size", 2)
            .get("/search/listings")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<SearchListingsResponse>::class.java)
            .toList()

        Assertions.assertEquals(2, list.size, "Expected exactly 2 search results")
        list.forEach { dto ->
            Assertions.assertNotNull(dto.id, "id should not be null")
            Assertions.assertNotNull(dto.title, "title should not be null")
            Assertions.assertNotNull(dto.description, "description should not be null")
            Assertions.assertTrue(
                dto.title.contains(token) || dto.title == targetTitle1 || dto.title == targetTitle2,
                "Title '${dto.title}' does not match expected token or target titles"
            )
        }
    }
}
