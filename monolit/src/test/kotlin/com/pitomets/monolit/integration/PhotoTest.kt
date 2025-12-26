package com.pitomets.monolit.integration

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PhotoTest : BaseContainers() {

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
        val downloadedImage = RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .get("/users/photos/avatar")
            .then()
            .statusCode(200)
            .contentType("image/jpeg")
            .extract()
            .asByteArray()

        assert(downloadedImage.isNotEmpty())
    }

    @Test
    fun `delete avatar test`() {
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

        // Удаляем аватар - контроллер возвращает 200 с телом
        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .delete("/users/photos/avatar")
            .then()
            .statusCode(200)
            .body("deletedAvatarKey", notNullValue())

        // Пытаемся скачать удаленный аватар - теперь вернет 404
        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .get("/users/photos/avatar")
            .then()
            .statusCode(404)
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

        // Загружаем первый аватар
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

        // Загружаем второй аватар (заменяем)
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

        // Ключи должны быть разными
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
    fun `download avatar without upload should fail`() {
        val email = faker.internet().emailAddress()
        val password = "Password123!"

        registerUser(email, password)
        val token = login(email, password)

        // Пытаемся скачать аватар, который не загружали - вернет 404
        RestAssured
            .given()
            .auth().oauth2(token.accessToken)
            .get("/users/photos/avatar")
            .then()
            .statusCode(404)
    }

    @Test
    fun `upload listing photo test`() {
        val sellerToken = createBaseSeller()

        // Создаем листинг
        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(
                """
                {
                    "title": "Test Pet",
                    "description": "Test description",
                    "species": "DOG",
                    "breed": "Labrador",
                    "ageMonths": 12,
                    "price": 1000
                }
                """.trimIndent()
            )
            .post("/listings/")
            .then()
            .statusCode(200)
            .extract()
            .path<Int>("listingsId")

        // Загружаем фото листинга
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

        // Создаем листинг
        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(
                """
                {
                    "title": "Test Pet",
                    "description": "Test description",
                    "species": "DOG",
                    "breed": "Labrador",
                    "ageMonths": 12,
                    "price": 1000
                }
                """.trimIndent()
            )
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
            .body(
                """
                {
                    "title": "Test Pet",
                    "description": "Test description",
                    "species": "DOG",
                    "breed": "Labrador",
                    "ageMonths": 12,
                    "price": 1000
                }
                """.trimIndent()
            )
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
        val downloadedImage = RestAssured
            .given()
            .get("/listings/$listingId/photos/$photoId")
            .then()
            .statusCode(200)
            .contentType("image/jpeg")
            .extract()
            .asByteArray()

        assert(downloadedImage.isNotEmpty())
    }

    @Test
    fun `delete listing photo test`() {
        val sellerToken = createBaseSeller()

        // Создаем листинг
        val listingId = RestAssured
            .given()
            .auth().oauth2(sellerToken.accessToken)
            .contentType(ContentType.JSON)
            .body(
                """
                {
                    "title": "Test Pet",
                    "description": "Test description",
                    "species": "DOG",
                    "breed": "Labrador",
                    "ageMonths": 12,
                    "price": 1000
                }
                """.trimIndent()
            )
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
            .body(
                """
                {
                    "title": "Test Pet",
                    "description": "Test description",
                    "species": "DOG",
                    "breed": "Labrador",
                    "ageMonths": 12,
                    "price": 1000
                }
                """.trimIndent()
            )
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
            .body(
                """
                {
                    "title": "Test Pet",
                    "description": "Test description",
                    "species": "DOG",
                    "breed": "Labrador",
                    "ageMonths": 12,
                    "price": 1000
                }
                """.trimIndent()
            )
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
            .body(
                """
                {
                    "title": "Test Pet with Photos",
                    "description": "Test description",
                    "species": "DOG",
                    "breed": "Labrador",
                    "ageMonths": 12,
                    "price": 1000
                }
                """.trimIndent()
            )
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
        val photos = RestAssured
            .given()
            .get("/listings/$listingId/photos")
            .then()
            .statusCode(200)
            .body("title", equalTo("Test Pet with Photos"))
            .body("photos.size()", equalTo(5))
            .extract()
            .path<List<String>>("photos")

        // Проверяем что фото в правильном порядке
        for (i in photoIds.indices) {
            assert(photos[i].contains("/listings/$listingId/photos/${photoIds[i]}"))
        }
    }
}
