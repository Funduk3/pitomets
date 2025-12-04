// package com.pitomets.monolit.integration
//
// import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
// import com.pitomets.monolit.model.dto.request.ListingsRequest
// import com.pitomets.monolit.model.dto.request.LoginRequest
// import com.pitomets.monolit.model.dto.request.RegisterRequest
// import com.pitomets.monolit.model.dto.response.ListingsResponse
// import com.pitomets.monolit.model.dto.response.TokenResponse
// import com.pitomets.monolit.model.dto.response.UserResponse
// import io.restassured.RestAssured
// import io.restassured.http.ContentType
// import net.datafaker.Faker
// import org.hamcrest.Matchers
// import org.junit.jupiter.api.Assertions
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.Test
// import org.junit.jupiter.api.TestInstance
// import org.springframework.boot.test.context.SpringBootTest
// import org.springframework.boot.test.web.server.LocalServerPort
// import org.springframework.test.context.ActiveProfiles
// import org.testcontainers.junit.jupiter.Testcontainers
// import java.math.BigDecimal
//
// @Testcontainers
// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
// @ActiveProfiles("test")
// class ListingsIntegrationTest : BaseContainers() {
//
//    @LocalServerPort
//    var port: Int = 0
//
//    val faker = Faker()
//
//    @BeforeEach
//    fun setUp() {
//        RestAssured.baseURI = "http://localhost"
//        RestAssured.port = port
//        println("Testing on port: $port")
//    }
//
//    private fun registerUser(email: String, password: String): UserResponse {
//        val registerReq = RegisterRequest(
//            email = email,
//            passwordHash = password,
//            fullName = faker.name().fullName()
//        )
//        return RestAssured.given()
//            .contentType(ContentType.JSON)
//            .body(registerReq)
//            .post("/register")
//            .then()
//            .statusCode(201)
//            .extract()
//            .`as`(UserResponse::class.java)
//    }
//
//    private fun login(email: String, password: String): TokenResponse {
//        val loginReq = LoginRequest(email = email, passwordHash = password)
//        return RestAssured.given()
//            .contentType(ContentType.JSON)
//            .body(loginReq)
//            .post("/login")
//            .then()
//            .statusCode(200)
//            .extract()
//            .`as`(TokenResponse::class.java)
//    }
//
//    @Test
//    fun `should create listing when seller profile exists`() {
//        val email = faker.internet().emailAddress()
//        val password = faker.internet().password(8, 16)
//
//        registerUser(email, password)
//        val tokens = login(email, password)
//
//        // create seller profile (become SELLER)
//        val createProfileReq = CreateSellerProfileRequest(
//            shopName = faker.company().name(),
//            description = "Seller shop"
//        )
//
//        RestAssured.given()
//            .contentType(ContentType.JSON)
//            .auth().oauth2(tokens.accessToken)
//            .body(createProfileReq)
//            .post("/seller/profile")
//            .then()
//            .statusCode(201)
//
//        // relogin to get updated roles/token if your app issues role at login
//        val sellerTokens = login(email, password)
//
//        val listingReq = ListingsRequest(
//            description = "Lovely puppy",
//            species = "dog",
//            breed = "labrador",
//            ageMonths = 2,
//            father = null,
//            mother = null,
//            price = BigDecimal(1500),
//        )
//
//        val created = RestAssured.given()
//            .contentType(ContentType.JSON)
//            .auth().oauth2(sellerTokens.accessToken)
//            .body(listingReq)
//            .post("/listings")
//            .then()
//            .statusCode(200) // controller returns ListingsResponse directly -> 200 OK
//            .extract()
//            .`as`(ListingsResponse::class.java)
//
//        // assertions
//        Assertions.assertEquals(listingReq.description, created.description)
//        Assertions.assertEquals(listingReq.species, created.species)
//        Assertions.assertEquals(listingReq.breed, created.breed)
//        Assertions.assertEquals(listingReq.ageMonths, created.ageMonths)
//        Assertions.assertEquals(listingReq.price, created.price)
//        Assertions.assertNotNull(created.listingsId)
//        Assertions.assertFalse(created.isArchived)
//    }
//
//    @Test
//    fun `should fail to create listing when seller profile does not exist`() {
//        val email = faker.internet().emailAddress()
//        val password = faker.internet().password(8, 16)
//
//        registerUser(email, password)
//        val tokens = login(email, password)
//
//        val listingReq = ListingsRequest(
//            description = "Orphan listing",
//            species = "cat",
//            breed = "mixed",
//            ageMonths = 1,
//            father = null,
//            mother = null,
//            price = BigDecimal(1500),
//        )
//
//        // without creating seller profile -> should return 4xx (user not found / forbidden)
//        RestAssured.given()
//            .contentType(ContentType.JSON)
//            .auth().oauth2(tokens.accessToken)
//            .body(listingReq)
//            .post("/listings")
//            .then()
//            .statusCode(
//                Matchers.allOf(
//                    Matchers.greaterThanOrEqualTo(400),
//                    Matchers.lessThan(500)
//                )
//            )
//    }
// }
