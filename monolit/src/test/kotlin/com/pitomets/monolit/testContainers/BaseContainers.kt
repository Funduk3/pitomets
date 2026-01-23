package com.pitomets.monolit.testContainers

import co.elastic.clients.elasticsearch.ElasticsearchClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pitomets.monolit.components.ListingOutboxProcessor
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.LoginRequest
import com.pitomets.monolit.model.dto.request.RegisterRequest
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.model.dto.response.UserResponse
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.service.SearchService
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.restassured.RestAssured
import io.restassured.http.ContentType
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
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

    @Autowired
    lateinit var elasticClient: ElasticsearchClient

    @Autowired
    lateinit var listingOutboxProcessor: ListingOutboxProcessor

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
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
        }

        @JvmStatic
        val elasticsearch = ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:9.2.4"
        ).apply {
            withEnv("discovery.type", "single-node")
            withEnv("xpack.security.enabled", "false")
            withEnv("xpack.security.transport.ssl.enabled", "false")
            withEnv("xpack.security.http.ssl.enabled", "false")
            withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            withExposedPorts(9200)
        }

        @JvmStatic
        val minio = GenericContainer<Nothing>("minio/minio:latest")
            .apply {
                withExposedPorts(9000, 9001)
                withEnv("MINIO_ROOT_USER", "minioadmin")
                withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                withCommand("server", "/data", "--console-address", ":9001")
                waitingFor(
                    HttpWaitStrategy()
                        .forPath("/minio/health/live")
                        .forPort(9000)
                        .withStartupTimeout(Duration.ofSeconds(60))
                )
            }

        init {
            postgres.start()
            redis.start()
            elasticsearch.start()
            minio.start()

            // Создаем bucket сразу после старта MinIO
            createMinioBucket()
        }

        private fun createMinioBucket() {
            val minioUrl = "http://${minio.host}:${minio.getMappedPort(9000)}"
            val client = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials("minioadmin", "minioadmin")
                .build()

            val bucketName = "test-bucket"
            val exists = client.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            )

            if (!exists) {
                client.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                )
                println("MinIO bucket '$bucketName' created")
            } else {
                println("MinIO bucket '$bucketName' already exists")
            }
        }

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))

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

            // MinIO
            registry.add("minio.url") {
                "http://${minio.host}:${minio.getMappedPort(9000)}"
            }
            registry.add("minio.access-key") { "minioadmin" }
            registry.add("minio.secret-key") { "minioadmin" }
            registry.add("minio.bucket") { "test-bucket" }

            // JWT
            val JWT_TEST_SECRET = Base64.getEncoder()
                .encodeToString("super-test-secret-key-which-is-long".toByteArray())
            registry.add("jwt.secret") { JWT_TEST_SECRET }

            registry.add("spring.kafka.bootstrap-servers") { BaseContainers.Companion.kafka.bootstrapServers }
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

    fun createBaseSeller(): TokenResponse {
        val email = faker.internet().emailAddress()
        val password = faker.internet().password(8, 16)
        registerUser(email, password)
        val tokens = login(email, password)
        val createRequest = CreateSellerProfileRequest(
            shopName = faker.company().name(),
            description = "Original description"
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().oauth2(tokens.accessToken)
            .body(createRequest)
            .post("/seller/profile")
            .then()
            .statusCode(201)
        return login(email, password)
    }

    fun createSomeListings(count: Int, sellerToken: TokenResponse) {
        repeat(count) {
            val req = ListingsRequest(
                description = faker.lorem().sentence(),
                species = faker.animal().name(),
                ageMonths = faker.number().numberBetween(1, 24),
                price = BigDecimal.valueOf(faker.number().numberBetween(1, 100).toLong()),
                breed = null,
                title = faker.book().title(),
                cityId = 4L
            )
            RestAssured.given()
                .contentType(ContentType.JSON)
                .auth().oauth2(sellerToken.accessToken)
                .body(req)
                .post("/listings/")
                .then()
                .statusCode(200)
        }
    }
}
