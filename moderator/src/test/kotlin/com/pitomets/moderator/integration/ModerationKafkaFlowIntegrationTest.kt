package com.pitomets.moderator.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pitomets.moderator.interfaces.messaging.event.ModerationEntityType
import com.pitomets.moderator.interfaces.messaging.event.ModerationOperation
import com.pitomets.moderator.interfaces.messaging.event.ModerationProcessedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationRequestedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationStatus
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.missing-topics-fatal=false",
        "spring.datasource.url=jdbc:h2:mem:moderator-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none"
    ]
)
@EmbeddedKafka(
    partitions = 1,
    topics = ["moderation.requested", "moderation.processed"]
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ModerationKafkaFlowIntegrationTest {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Value("\${spring.embedded.kafka.brokers}")
    private lateinit var kafkaBrokers: String

    private lateinit var consumerContainer: KafkaMessageListenerContainer<String, ModerationProcessedEvent>
    private lateinit var processedEvents: BlockingQueue<ConsumerRecord<String, ModerationProcessedEvent>>

    @BeforeEach
    fun setUpConsumer() {
        processedEvents = LinkedBlockingQueue()
        consumerContainer = createProcessedEventsConsumer(processedEvents)
        consumerContainer.start()
        ContainerTestUtils.waitForAssignment(consumerContainer, 1)
    }

    @AfterEach
    fun tearDownConsumer() {
        if (::consumerContainer.isInitialized) {
            consumerContainer.stop()
        }
    }

    @Test
    fun `should consume kafka event call API and publish moderation result`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "decision": {
                        "action": "reject",
                        "mode": "strong",
                        "reason": "toxicity >= 0.8 or explicit profanity detected"
                      },
                      "categories": {
                        "profanity": {"detected": true, "matches": ["..."]},
                        "sexual_content": {"detected": false, "matches": []},
                        "toxicity": {"score": 0.91}
                      },
                      "usage": {"request_tokens": 37, "remaining_tokens": 18760},
                      "meta": {
                        "request_id": "4f80e644-5dbe-4f75-b63c-fbf0525c7848",
                        "processing_time_ms": 118,
                        "model_version": "v1.2.0"
                      }
                    }
                    """.trimIndent()
                )
        )

        val requestEvent = ModerationRequestedEvent(
            eventId = UUID.randomUUID(),
            entityType = ModerationEntityType.LISTING,
            entityId = 101L,
            operation = ModerationOperation.CREATE,
            textParts = listOf("Продам щенка", "Очень умный, но мат и запрещенка"),
            withAnimal = false
        )

        kafkaTemplate.send("moderation.requested", "LISTING:101", requestEvent).get(5, TimeUnit.SECONDS)

        val processedRecord = processedEvents.poll(10, TimeUnit.SECONDS)
        assertThat(processedRecord).isNotNull

        val processedEvent = processedRecord!!.value()
        assertThat(processedEvent.requestEventId).isEqualTo(requestEvent.eventId)
        assertThat(processedEvent.entityType).isEqualTo(ModerationEntityType.LISTING)
        assertThat(processedEvent.entityId).isEqualTo(101L)
        assertThat(processedEvent.status).isEqualTo(ModerationStatus.REJECTED)
        assertThat(processedEvent.reason).contains("toxicity")
        assertThat(processedEvent.toxicityScore).isEqualTo(0.91)
        assertThat(processedEvent.modelVersion).isEqualTo("v1.2.0")

        val apiRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertThat(apiRequest).isNotNull
        assertThat(apiRequest!!.path).isEqualTo("/api/v1/analyze")
        assertThat(apiRequest.getHeader("X-API-Token")).isEqualTo("integration-test-token")

        val payload = objectMapper.readTree(apiRequest.body.readUtf8())
        assertThat(payload["mode"].asText()).isEqualTo("strong")
        assertThat(payload["with_animal"].asBoolean()).isFalse()
        assertThat(payload["text"].asText()).contains("Продам щенка")
        assertThat(payload["text"].asText()).contains("запрещенка")
    }

    @Test
    fun `should publish error result when external API returns HTTP error`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"error":"internal"}""")
        )

        val requestEvent = ModerationRequestedEvent(
            eventId = UUID.randomUUID(),
            entityType = ModerationEntityType.REVIEW,
            entityId = 777L,
            operation = ModerationOperation.UPDATE,
            textParts = listOf("Отзыв с потенциальным нарушением"),
            withAnimal = true
        )

        kafkaTemplate.send("moderation.requested", "REVIEW:777", requestEvent).get(5, TimeUnit.SECONDS)

        val processedRecord = processedEvents.poll(10, TimeUnit.SECONDS)
        assertThat(processedRecord).isNotNull

        val processedEvent = processedRecord!!.value()
        assertThat(processedEvent.requestEventId).isEqualTo(requestEvent.eventId)
        assertThat(processedEvent.entityType).isEqualTo(ModerationEntityType.REVIEW)
        assertThat(processedEvent.entityId).isEqualTo(777L)
        assertThat(processedEvent.status).isEqualTo(ModerationStatus.ERROR)
        assertThat(processedEvent.reason).contains("HTTP error")

        val apiRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertThat(apiRequest).isNotNull
        assertThat(apiRequest!!.path).isEqualTo("/api/v1/analyze")
    }

    private fun createProcessedEventsConsumer(
        records: BlockingQueue<ConsumerRecord<String, ModerationProcessedEvent>>
    ): KafkaMessageListenerContainer<String, ModerationProcessedEvent> {
        val uniqueGroupId = "moderator-integration-test-${System.currentTimeMillis()}-${System.nanoTime()}"

        val consumerProps = KafkaTestUtils.consumerProps(kafkaBrokers, uniqueGroupId).toMutableMap()
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        consumerProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false

        val valueDeserializer = JacksonJsonDeserializer(ModerationProcessedEvent::class.java).apply {
            addTrustedPackages("*")
            setUseTypeHeaders(false)
        }
        val consumerFactory = DefaultKafkaConsumerFactory(
            consumerProps,
            StringDeserializer(),
            valueDeserializer
        )
        val containerProperties = ContainerProperties("moderation.processed")

        return KafkaMessageListenerContainer(consumerFactory, containerProperties).apply {
            setupMessageListener(
                MessageListener<String, ModerationProcessedEvent> { record ->
                    records.add(record)
                }
            )
        }
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
        private val mockWebServer = MockWebServer().apply { start() }

        @JvmStatic
        @AfterAll
        fun tearDownMockServer() {
            mockWebServer.shutdown()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("moderium.api.base-url") { mockWebServer.url("/").toString().removeSuffix("/") }
            registry.add("moderium.api.token") { "integration-test-token" }
            registry.add("moderium.api.mode") { "strong" }
            registry.add("moderium.api.with-animal") { true }
        }
    }
}
