package com.pitomets.monolit.integration

import com.pitomets.monolit.model.Gender
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.testContainers.BaseContainers
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers
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
class SellerProfileTest : BaseContainers() {

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
        approveSellerProfile(created.userId ?: error("Seller user id missing"))

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
        approveSellerProfile(updated.userId ?: error("Seller user id missing"))

        // Проверки
        Assertions.assertEquals(created.id, updated.id)
        Assertions.assertEquals(updatedShopName, updated.shopName)
        Assertions.assertEquals("Updated description", updated.description)
        Assertions.assertNotEquals(originalShopName, updated.shopName)

        // Создаем объявление
        val createListingRequest = ListingsRequest(
            description = faker.funnyName().name(),
            species = faker.funnyName().name(),
            ageMonths = faker.number().numberBetween(1, 3),
            price = BigDecimal(faker.number().randomDigit()),
            breed = null,
            title = faker.name().fullName(),
            cityId = 4L,
            metroId = null,
            gender = Gender.M,
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(sellerTokens.accessToken)
            .body(createListingRequest)
            .post("/listings/")
            .then()
            .statusCode(200)

        // Проверяем что сохранилось объявление в БД
        val allListings = listingsRepo.findAll()
        Assertions.assertTrue(
            allListings.isNotEmpty(),
            "Listings should not be empty"
        )

        val createdListing = allListings.find {
            it.description == createListingRequest.description
        }
        Assertions.assertNotNull(
            createdListing,
            "Created listing should exist in DB"
        )
        Assertions.assertEquals(
            createListingRequest.species,
            createdListing!!.species
        )

        val adminToken = loginAdmin()
        RestAssured.given()
            .auth().oauth2(adminToken.accessToken)
            .post("/admin/listing/${createdListing.id}/approve")
            .then()
            .statusCode(200)

        // Найти объявление без ауф токена
        RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", createdListing.id) // query params
            .get("/listings/")
            .then()
            .statusCode(200)
            .body("description", Matchers.equalTo(createListingRequest.description))
            .body("species", Matchers.equalTo(createListingRequest.species))

        val newDescription = faker.funnyName().name()
        val updateListingRequest = UpdateListingRequest(
            newDescription,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            4L,
            null,
        )
        // Исправить объявление без прав
        RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", createdListing.id)
            .body(updateListingRequest)
            .put("/listings/")
            .then()
            .statusCode(401)

        // Success исправить объявление
        RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", createdListing.id)
            .auth().oauth2(sellerTokens.accessToken)
            .body(updateListingRequest)
            .put("/listings/")
            .then()
            .statusCode(200)
            .body(
                "description",
                Matchers.equalTo(newDescription)
            )

        Assertions.assertEquals(
            newDescription,
            listingsRepo.findById(createdListing.id!!).get().description
        )

        // Добавить объявление в избранное
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("listingId" to createdListing.id))
            .auth().oauth2(tokens.accessToken)
            .post("/favourites")
            .then()
            .statusCode(200)

        // Получить мои избранные
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .get("/favourites")
            .then()
            .statusCode(200)
            .body("[0].description", Matchers.equalTo(newDescription))

        // удаляем избранное
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("listingId" to createdListing.id))
            .auth().oauth2(tokens.accessToken)
            .delete("/favourites")
            .then()
            .statusCode(200)

        // Получить мои избранные (ожидаем пустоту)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .get("/favourites")
            .then()
            .statusCode(200)
            .body("size()", Matchers.equalTo(0))

        // Success удалить объявление
        RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", createdListing.id)
            .auth().oauth2(sellerTokens.accessToken)
            .body(updateListingRequest)
            .delete("/listings/")
            .then()
            .statusCode(200)

        // не найдем объявление после удаления
        RestAssured.given()
            .contentType(ContentType.JSON)
            .param("id", createdListing.id)
            .get("/listings/")
            .then()
            .statusCode(404)
        // не найдём объявление в поиске
        listingOutboxProcessor.processOutbox()
        elasticClient.indices().refresh { r -> r.index("listings") }
        val searchResults = searchService.search(createdListing.description)
        Assertions.assertTrue(
            searchResults.items.none { it.id == createdListing.id },
            "Deleted listing should not appear in search results"
        )
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
