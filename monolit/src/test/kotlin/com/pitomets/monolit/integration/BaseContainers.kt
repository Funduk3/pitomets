package com.pitomets.monolit.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pitomets.monolit.model.dto.request.LoginRequest
import com.pitomets.monolit.model.dto.request.RegisterRequest
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.model.dto.response.UserResponse
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.service.SearchService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Base64

@Suppress("UtilityClassWithPublicConstructor")
@Testcontainers
abstract class BaseContainers {

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        println("Testing on port: $port")
    }

    @Autowired
    lateinit var listingsRepo: ListingsRepo

    @Autowired
    lateinit var searchService: SearchService

    companion object {

        val faker = Faker()

        val mapper = jacksonObjectMapper().apply {
            findAndRegisterModules()
        }

        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine")
            .apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }

        @JvmStatic
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply { withExposedPorts(6379) }

        @JvmStatic
        val elasticsearch = ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:8.15.0"
        ).apply {
            withEnv("discovery.type", "single-node")
            withEnv("xpack.security.enabled", "false")
            withEnv("xpack.security.transport.ssl.enabled", "false")
            withEnv("xpack.security.http.ssl.enabled", "false")
            withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            withExposedPorts(9200)
        }

        init {
            postgres.start()
            redis.start()
            elasticsearch.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            // Postgres
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }

            // Redis
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort.toString() }

            // Elasticsearch
            registry.add("spring.elasticsearch.uris") {
                "http://${elasticsearch.host}:${elasticsearch.firstMappedPort}"
            }

            // JWT
            val JWT_TEST_SECRET = Base64.getEncoder()
                .encodeToString("super-test-secret-key-which-is-long".toByteArray())
            registry.add("jwt.secret") { JWT_TEST_SECRET }
        }
    }

    fun registerUser(email: String, password: String): UserResponse {
        val registerReq = RegisterRequest(
            email = email,
            passwordHash = password,
            fullName = faker.name().fullName()
        )
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registerReq)
            .post("/register")
            .then()
            .statusCode(201)
            .extract()
            .`as`(UserResponse::class.java)
    }

    fun login(email: String, password: String): TokenResponse {
        val loginReq = LoginRequest(email = email, passwordHash = password)
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginReq)
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .`as`(TokenResponse::class.java)
    }
}
