package com.pitomets.monolit.integration

import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingReviewRequest
import com.pitomets.monolit.model.dto.request.UpdateSellerReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.testContainers.BaseContainers
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNotNull
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class ReviewsTest : BaseContainers() {

    @Test
    fun `should create and retrieve listing review successfully`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)

        val createRequest = CreateReviewRequest(
            listingId = listingId,
            rating = 5,
            text = "Great listing!"
        )

        val reviewResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .body(createRequest)
            .post("/listings/reviews/")
            .then()
            .statusCode(201)
            .extract()
            .`as`(ReviewResponse::class.java)

        assertNotNull(reviewResponse.id)
        assertEquals(5, reviewResponse.rating)
        assertEquals("Great listing!", reviewResponse.text)
        assertEquals(listingId, reviewResponse.listingId)
    }

    @Test
    fun `should get all reviews for a listing`() {
        val buyer1Token = createBuyer()
        val buyer2Token = createBuyer()
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)

        createListingReview(buyer1Token, listingId, 5, "Excellent!")
        createListingReview(buyer2Token, listingId, 4, "Very good")

        val reviews = RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", listingId)
            .get("/listings/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<ReviewResponse>::class.java)

        assertEquals(2, reviews.size)
        assertTrue(reviews.any { it.rating == 5 && it.text == "Excellent!" })
        assertTrue(reviews.any { it.rating == 4 && it.text == "Very good" })
    }

    @Test
    fun `should update listing review successfully`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)

        val originalReview = createListingReview(buyerToken, listingId, 3, "Average")

        val updateRequest = UpdateListingReviewRequest(
            listingId = listingId,
            rating = 5,
            text = "Actually it's great!",
            authorId = originalReview.authorId
        )

        val updatedReview = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .body(updateRequest)
            .put("/listings/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(ReviewResponse::class.java)

        assertEquals(originalReview.id, updatedReview.id)
        assertEquals(5, updatedReview.rating)
        assertEquals("Actually it's great!", updatedReview.text)
    }

    @Test
    fun `should delete listing review successfully`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)

        val review = createListingReview(buyerToken, listingId, 4, "Good")

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .param("sellerProfileId", review.sellerProfileId)
            .delete("/listings/reviews/${review.id}")
            .then()
            .statusCode(200)

        val reviews = RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", listingId)
            .get("/listings/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<ReviewResponse>::class.java)

        assertEquals(0, reviews.size)
    }

    @Test
    fun `should not allow seller to review their own listing`() {
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)

        val createRequest = CreateReviewRequest(
            listingId = listingId,
            rating = 5,
            text = "My own listing is great!"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerToken.accessToken)
            .body(createRequest)
            .post("/listings/reviews/")
            .then()
            .statusCode(400)
    }

    @Test
    fun `should not allow duplicate reviews from same user on same listing`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)

        createListingReview(buyerToken, listingId, 5, "First review")

        val duplicateRequest = CreateReviewRequest(
            listingId = listingId,
            rating = 4,
            text = "Trying to review again"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .body(duplicateRequest)
            .post("/listings/reviews/")
            .then()
            .statusCode(400)
    }

    @Test
    fun `should not allow user to update another user's review`() {
        val buyer1Token = createBuyer()
        val buyer2Token = createBuyer()
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)

        val review = createListingReview(buyer1Token, listingId, 5, "Original review")

        val updateRequest = UpdateListingReviewRequest(
            listingId = listingId,
            rating = 1,
            text = "Hacked review",
            authorId = review.authorId
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyer2Token.accessToken)
            .body(updateRequest)
            .put("/listings/reviews/")
            .then()
            .statusCode(400)
    }

    @Test
    fun `should create and retrieve seller review successfully`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val sellerProfileId = getSellerProfileId(sellerToken)

        val createRequest = CreateReviewRequest(
            listingId = 0, // Not used for seller reviews
            rating = 5,
            text = "Excellent seller!"
        )

        val reviewResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .body(createRequest)
            .post("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(201)
            .extract()
            .`as`(ReviewResponse::class.java)

        assertNotNull(reviewResponse.id)
        assertEquals(5, reviewResponse.rating)
        assertEquals("Excellent seller!", reviewResponse.text)
        assertNull(reviewResponse.listingId)
        assertEquals(sellerProfileId, reviewResponse.sellerProfileId)
    }

    @Test
    fun `should get all reviews for a seller`() {
        val buyer1Token = createBuyer()
        val buyer2Token = createBuyer()
        val sellerToken = createBaseSeller()
        val sellerProfileId = getSellerProfileId(sellerToken)

        createSellerReview(buyer1Token, sellerProfileId, 5, "Great seller!")
        createSellerReview(buyer2Token, sellerProfileId, 4, "Good communication")

        val reviews = RestAssured.given()
            .contentType(ContentType.JSON)
            .get("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<ReviewResponse>::class.java)

        assertEquals(2, reviews.size)
        assertTrue(reviews.all { it.sellerProfileId == sellerProfileId })
        assertTrue(reviews.all { it.listingId == null })
    }

    @Test
    fun `should update seller review successfully`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val sellerProfileId = getSellerProfileId(sellerToken)

        val originalReview = createSellerReview(buyerToken, sellerProfileId, 3, "OK seller")

        val updateRequest = UpdateSellerReviewRequest(
            rating = 5,
            text = "Changed my mind, excellent seller!",
            authorId = originalReview.authorId,
        )

        val updatedReview = RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .body(updateRequest)
            .put("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(ReviewResponse::class.java)

        assertEquals(5, updatedReview.rating)
        assertEquals("Changed my mind, excellent seller!", updatedReview.text)
    }

    @Test
    fun `should delete seller review successfully`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val sellerProfileId = getSellerProfileId(sellerToken)

        val review = createSellerReview(buyerToken, sellerProfileId, 4, "Good")

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .delete("/seller/$sellerProfileId/reviews/${review.id}")
            .then()
            .statusCode(200)

        val reviews = RestAssured.given()
            .contentType(ContentType.JSON)
            .get("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<ReviewResponse>::class.java)

        assertEquals(0, reviews.size)
    }

    @Test
    fun `should not allow seller to review themselves`() {
        val sellerToken = createBaseSeller()
        val sellerProfileId = getSellerProfileId(sellerToken)

        val createRequest = CreateReviewRequest(
            listingId = 0,
            rating = 5,
            text = "I'm great!"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerToken.accessToken)
            .body(createRequest)
            .post("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(400)
    }

    @Test
    fun `should get both listing and seller reviews for seller profile`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)
        val sellerProfileId = getSellerProfileId(sellerToken)

        // Create listing review
        createListingReview(buyerToken, listingId, 5, "Great listing!")

        // Create seller review
        createSellerReview(buyerToken, sellerProfileId, 4, "Good seller")

        val sellerReviews = RestAssured.given()
            .contentType(ContentType.JSON)
            .get("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<ReviewResponse>::class.java)

        // Should have both reviews
        assertEquals(2, sellerReviews.size)
        assertTrue(sellerReviews.any { it.listingId != null })
        assertTrue(sellerReviews.any { it.listingId == null })
    }

    // Helper methods
    private fun createBuyer(): TokenResponse {
        val email = faker.internet().emailAddress()
        val password = faker.internet().password(8, 16)
        registerUser(email, password)
        return login(email, password)
    }

    private fun createListing(sellerToken: TokenResponse): Long {
        val req = ListingsRequest(
            description = faker.lorem().sentence(),
            species = faker.animal().name(),
            ageMonths = faker.number().numberBetween(1, 24),
            price = BigDecimal.valueOf(faker.number().numberBetween(1, 100).toLong()),
            breed = null,
            title = faker.book().title(),
            cityId = 4L,
            metroId = null
        )
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerToken.accessToken)
            .body(req)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("listingsId")
    }

    private fun createListingReview(
        buyerToken: TokenResponse,
        listingId: Long,
        rating: Int,
        text: String
    ): ReviewResponse {
        val createRequest = CreateReviewRequest(
            listingId = listingId,
            rating = rating,
            text = text
        )
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .body(createRequest)
            .post("/listings/reviews/")
            .then()
            .statusCode(201)
            .extract()
            .`as`(ReviewResponse::class.java)
    }

    private fun createSellerReview(
        buyerToken: TokenResponse,
        sellerProfileId: Long,
        rating: Int,
        text: String
    ): ReviewResponse {
        val createRequest = CreateReviewRequest(
            listingId = 0,
            rating = rating,
            text = text
        )
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyerToken.accessToken)
            .body(createRequest)
            .post("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(201)
            .extract()
            .`as`(ReviewResponse::class.java)
    }

    private fun getSellerProfileId(sellerToken: TokenResponse): Long {
        // Assuming you have an endpoint to get seller profile or extract from listing
        val listingId = createListing(sellerToken)
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", listingId)
            .get("/listings/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("[0].sellerProfileId")
    }
}
