package com.pitomets.monolit.integration

import com.pitomets.monolit.model.Gender
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.testContainers.BaseContainers
import io.restassured.RestAssured
import io.restassured.config.RedirectConfig
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PhotoTest : BaseContainers() {

    val createListingRequest = ListingsRequest(
        description = faker.lorem().sentence(),
        species = faker.animal().name(),
        ageMonths = faker.number().numberBetween(1, 3),
        price = BigDecimal.valueOf(faker.number().numberBetween(1, 100).toLong()),
        breed = null,
        title = faker.book().title(),
        cityId = 4L,
        gender = Gender.M
    )

    @Test
    fun `upload avatar test`() {
        val email = faker.internet().emailAddress()
        val password = "Password123!"

        registerUser(email, password)
        val token = login(email, password)

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "ava.jpg", image, "image/jpeg")
            .post("/users/photos/avatar")
            .then()
            .statusCode(201)
            .body("avatarKey", notNullValue())
    }

    @Test
    fun `download avatar test`() {
        val email = faker.internet().emailAddress()
        val password = "Password123!"

        registerUser(email, password)
        val token = login(email, password)

        // Загружаем аватар
        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "ava.jpg", image, "image/jpeg")
            .post("/users/photos/avatar")
            .then()
            .statusCode(201)

        // Скачиваем аватар
        val avatarUrl = RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .get("/users/photos/avatar")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .path<String>("url")

        assert(!avatarUrl.isNullOrBlank())
        assert(avatarUrl.contains("/objects/avatars/"))
    }

    @Test
    fun `delete avatar test`() {
        val email = faker.internet().emailAddress()
        val password = "Password123!"

        registerUser(email, password)
        val token = login(email, password)

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "ava.jpg", image, "image/jpeg")
            .post("/users/photos/avatar")
            .then()
            .statusCode(201)

        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .delete("/users/photos/avatar")
            .then()
            .statusCode(200)

        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .get("/users/photos/avatar")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("url", equalTo(null))
    }

    @Test
    fun `replace avatar test`() {
        val email = faker.internet().emailAddress()
        val password = "Password123!"

        registerUser(email, password)
        val token = login(email, password)

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        val firstKey = RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "ava.jpg", image, "image/jpeg")
            .post("/users/photos/avatar")
            .then()
            .statusCode(201)
            .extract()
            .path<String>("avatarKey")

        val secondKey = RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "ava2.jpg", image, "image/jpeg")
            .post("/users/photos/avatar")
            .then()
            .statusCode(201)
            .extract()
            .path<String>("avatarKey")

        assert(firstKey != secondKey)
    }

    @Test
    fun `upload avatar without authentication should fail`() {
        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        RestAssured
            .given()
            .contentType("multipart/form-data")
            .multiPart("file", "ava.jpg", image, "image/jpeg")
            .post("/users/photos/avatar")
            .then()
            .statusCode(401)
    }

    @Test
    fun `download avatar without upload should return null url`() {
        val email = faker.internet().emailAddress()
        val password = "Password123!"

        registerUser(email, password)
        val token = login(email, password)

        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .get("/users/photos/avatar")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("url", equalTo(null))
    }

    @Test
    fun `upload listing photo test`() {
        val sellerToken = createBaseSeller()

        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path<Int>("listingsId")

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "listing.jpg", image, "image/jpeg")
            .post("/listings/$listingId/photos")
            .then()
            .statusCode(201)
            .body("photoId", notNullValue())
            .body("objectKey", notNullValue())
            .body("position", equalTo(0))
    }

    @Test
    fun `upload multiple listing photos test`() {
        val sellerToken = createBaseSeller()

        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path<Int>("listingsId")

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        // Загружаем 3 фото
        for (i in 0..2) {
            RestAssured
                .given()
                .auth().oauth2(sellerToken.accessToken)
                .contentType("multipart/form-data")
                .multiPart("file", "photo$i.jpg", image, "image/jpeg")
                .post("/listings/$listingId/photos")
                .then()
                .statusCode(201)
                .body("position", equalTo(i))
        }

        // Получаем список фото
        RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .get("/listings/$listingId/photos")
            .then()
            .statusCode(200)
            .body("photos.size()", equalTo(3))
    }

    @Test
    fun `download listing photo test`() {
        val sellerToken = createBaseSeller()

        // Создаем листинг
        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path<Int>("listingsId")

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        // Загружаем фото
        val photoId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "listing.jpg", image, "image/jpeg")
            .post("/listings/$listingId/photos")
            .then()
            .statusCode(201)
            .extract()
            .path<Int>("photoId")

        // Скачиваем фото (без аутентификации - публичный доступ)
        val redirectLocation = RestAssured
            .given()
            .config(
                RestAssured.config()
                    .redirect(RedirectConfig.redirectConfig().followRedirects(false))
            )
            .get("/listings/$listingId/photos/$photoId")
            .then()
            .statusCode(302)
            .extract()
            .header("Location")

        assert(!redirectLocation.isNullOrBlank())
        assert(redirectLocation.contains("/objects/listings/$listingId/"))
    }

    @Test
    fun `delete listing photo test`() {
        val sellerToken = createBaseSeller()

        // Создаем листинг
        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path<Int>("listingsId")

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        // Загружаем фото
        val photoId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "listing.jpg", image, "image/jpeg")
            .post("/listings/$listingId/photos")
            .then()
            .statusCode(201)
            .extract()
            .path<Int>("photoId")

        // Удаляем фото
        RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .delete("/listings/$listingId/photos/$photoId")
            .then()
            .statusCode(204)

        // Пытаемся скачать удаленное фото - вернет 404
        RestAssured
            .given()
            .get("/listings/$listingId/photos/$photoId")
            .then()
            .statusCode(404)
    }

    @Test
    fun `non-owner cannot upload listing photo`() {
        val sellerToken = createBaseSeller()
        val otherUserToken = createBaseSeller() // Другой продавец

        // Создаем листинг первым продавцом
        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path<Int>("listingsId")

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        // Пытаемся загрузить фото другим продавцом
        RestAssured
            .given()
            .auth().oauth2(otherUserToken.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "listing.jpg", image, "image/jpeg")
            .post("/listings/$listingId/photos")
            .then()
            .statusCode(403) // Access Denied
    }

    @Test
    fun `non-owner cannot delete listing photo`() {
        val sellerToken = createBaseSeller()
        val otherUserToken = createBaseSeller()

        // Создаем листинг первым продавцом
        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path<Int>("listingsId")

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        // Загружаем фото первым продавцом
        val photoId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType("multipart/form-data")
            .multiPart("file", "listing.jpg", image, "image/jpeg")
            .post("/listings/$listingId/photos")
            .then()
            .statusCode(201)
            .extract()
            .path<Int>("photoId")

        // Пытаемся удалить фото другим продавцом
        RestAssured
            .given()
            .auth().oauth2(otherUserToken.accessToken)
            .delete("/listings/$listingId/photos/$photoId")
            .then()
            .statusCode(403) // Access Denied
    }

    @Test
    fun `get listing photos returns correct order`() {
        val sellerToken = createBaseSeller()

        // Создаем листинг
        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path<Int>("listingsId")

        val image = javaClass
            .getResourceAsStream("/ava.jpg")!!
            .readBytes()

        // Загружаем 5 фото
        val photoIds = mutableListOf<Int>()
        for (i in 0..4) {
            val photoId = RestAssured
                .given()
                .auth().oauth2(sellerToken.accessToken)
                .contentType("multipart/form-data")
                .multiPart("file", "photo$i.jpg", image, "image/jpeg")
                .post("/listings/$listingId/photos")
                .then()
                .statusCode(201)
                .extract()
                .path<Int>("photoId")
            photoIds.add(photoId)
        }

        // Проверяем порядок
        val response = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .get("/listings/$listingId/photos")
            .then()
            .statusCode(200)
            .body("title", equalTo(createListingRequest.title))
            .body("photos.size()", equalTo(5))
            .extract()
        val photos = response.path<List<String>>("photos")
        val returnedPhotoIds = response.path<List<Int>>("photoIds")

        // Проверяем порядок id
        assert(returnedPhotoIds == photoIds)
        // Проверяем, что ссылки ведут к объектам листинга
        for (photoUrl in photos) {
            assert(photoUrl.contains("/objects/listings/$listingId/"))
        }
    }
}
