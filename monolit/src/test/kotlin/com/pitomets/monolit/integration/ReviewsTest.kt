package com.pitomets.monolit.integration

import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.request.ListingsRequest
import io.restassured.RestAssured
import io.restassured.http.ContentType
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
class ReviewsTest : BaseContainers() {
    @Test
    fun `Create a lot of listings and search should return most similar`() {
        val email = faker.internet().emailAddress()
        val password = faker.internet().password(8, 16)
        registerUser(email, password)
        val token = login(email, password) // buyer token
        val tokenSeller = createBaseSeller() // seller token

        val req = ListingsRequest(
            description = faker.lorem().sentence(),
            species = faker.animal().name(),
            ageMonths = faker.number().numberBetween(1, 24),
            price = BigDecimal.valueOf(faker.number().numberBetween(1, 100).toLong()),
            breed = null,
            title = faker.book().title()
        )
        val listingId = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokenSeller.accessToken)
            .body(req)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("listingsId")

        // создать отзыв
        val createReviewRequest = CreateReviewRequest(
            listingId = listingId,
            rating = faker.number().numberBetween(1, 5),
            text = faker.lorem().sentence(),
        )
        val reviewResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(token.accessToken)
            .body(createReviewRequest)
            .post("/listings/reviews")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("authorId")

        // все отзывы объявления
        RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", listingId)
            .get("/listings/reviews")
            .then()
            .statusCode(200)

        // все отзывы продавца
        RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", reviewResponse)
            .get("/seller/reviews")
            .then()
            .statusCode(200)
    }
}
