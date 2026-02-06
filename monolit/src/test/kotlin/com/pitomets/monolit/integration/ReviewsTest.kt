package com.pitomets.monolit.integration

import com.pitomets.monolit.model.Gender
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.testContainers.BaseContainers
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    private lateinit var sellerProfileRepo: SellerProfileRepo

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
    fun `should forbid updating another review`() {
        val buyer1 = createBuyer()
        val buyer2 = createBuyer()
        val seller = createBaseSeller()
        val listingId = createListing(seller)

        createListingReview(buyer1, listingId, 3, "ok")

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(buyer2.accessToken)
            .body(
                UpdateListingReviewRequest(
                    listingId = listingId,
                    rating = 1,
                    text = "hacked"
                )
            )
            .put("/listings/reviews/")
            .then()
            .statusCode(400)
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
    fun `should get all reviews for a seller`() {
        val buyer1Token = createBuyer()
        val buyer2Token = createBuyer()
        val sellerToken = createBaseSeller()
        val sellerProfileId = getSellerProfileId(sellerToken)
        val listingId = createListing(sellerToken)

        val review1 = createListingReview(buyer1Token, listingId, 5, "Great listing!")
        createListingReview(buyer2Token, listingId, 5, "Great listing!")

        val reviews = RestAssured.given()
            .contentType(ContentType.JSON)
            .get("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<ReviewResponse>::class.java)

        assertEquals(2, reviews.size)
        val actualSellerProfileId = review1.sellerProfileId
        assertTrue(reviews.all { it.sellerProfileId == actualSellerProfileId })
    }

    @Test
    fun `should get listing review for seller profile`() {
        val buyerToken = createBuyer()
        val sellerToken = createBaseSeller()
        val listingId = createListing(sellerToken)
        val sellerProfileId = getSellerProfileId(sellerToken)

        createListingReview(buyerToken, listingId, 5, "Great listing!")

        val sellerReviews = RestAssured.given()
            .contentType(ContentType.JSON)
            .get("/seller/$sellerProfileId/reviews/")
            .then()
            .statusCode(200)
            .extract()
            .`as`(Array<ReviewResponse>::class.java)

        assertEquals(1, sellerReviews.size)
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
            ageMonths = faker.number().numberBetween(1, 3),
            price = BigDecimal.valueOf(faker.number().numberBetween(1, 100).toLong()),
            breed = null,
            title = faker.book().title(),
            cityId = 4L,
            metroId = null,
            gender = Gender.M,
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

    private fun getSellerProfileId(sellerToken: TokenResponse): Long {
        val userId = RestAssured.given()
            .auth().oauth2(sellerToken.accessToken)
            .get("/profile/me")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("id")
        return sellerProfileRepo.findBySellerId(userId)?.id!!
    }
}
