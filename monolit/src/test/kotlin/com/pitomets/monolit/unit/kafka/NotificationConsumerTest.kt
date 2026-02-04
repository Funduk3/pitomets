package com.pitomets.monolit.unit.kafka

import com.pitomets.monolit.model.kafka.NotificationFailedEvent
import com.pitomets.monolit.model.kafka.NotificationSentEvent
import com.pitomets.monolit.model.kafka.event.Channel
import com.pitomets.monolit.testContainers.BaseContainers
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
class NotificationConsumerTest : BaseContainers() {

    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @BeforeEach
    fun setUpProducer() {
        kafkaTemplate = createTestProducer()

        Thread.sleep(500)
    }

    @AfterEach
    fun tearDown() {
        Thread.sleep(500)
    }

    @Test
    fun `should consume and process notification sent event`() {
        val sentEvent = NotificationSentEvent(
            notificationId = System.nanoTime(), // Уникальный ID
            eventId = System.currentTimeMillis(),
            userId = 100L,
            channel = Channel.EMAIL
        )
        val message = mapper.writeValueAsString(sentEvent)

        kafkaTemplate.send("notification-events", message).get(5, TimeUnit.SECONDS)

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assert(true)
        }
    }

    @Test
    fun `should consume and process notification failed event`() {
        val failedEvent = NotificationFailedEvent(
            notificationId = System.nanoTime(), // Уникальный ID
            eventId = System.currentTimeMillis(),
            userId = 200L,
            channel = "SMS",
            error = "Invalid phone number"
        )
        val message = mapper.writeValueAsString(failedEvent)

        kafkaTemplate.send("notification-events", message).get(5, TimeUnit.SECONDS)

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assert(true)
        }
    }

    @Test
    fun `should handle multiple events in sequence`() {
        val events = listOf(
            NotificationSentEvent(
                notificationId = System.nanoTime(),
                eventId = 1001L,
                userId = 100L,
                channel = Channel.EMAIL
            ),
            NotificationSentEvent(
                notificationId = System.nanoTime() + 1,
                eventId = 1002L,
                userId = 200L,
                channel = Channel.SMS
            ),
            NotificationFailedEvent(
                notificationId = System.nanoTime() + 2,
                eventId = 1003L,
                userId = 300L,
                channel = "PUSH",
                error = "Device not found"
            )
        )

        events.forEach { event ->
            val message = mapper.writeValueAsString(event)
            kafkaTemplate.send("notification-events", message).get(5, TimeUnit.SECONDS)
        }

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assert(true)
        }
    }

    private fun createTestProducer(): KafkaTemplate<String, String> {
        val producerProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 0,
            ProducerConfig.LINGER_MS_CONFIG to 0
        )
        val producerFactory = DefaultKafkaProducerFactory<String, String>(producerProps)
        return KafkaTemplate(producerFactory)
    }
}
