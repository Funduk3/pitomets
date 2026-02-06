package com.pitomets.notifications

import com.fasterxml.jackson.databind.ObjectMapper
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.Status
import com.pitomets.notifications.domain.port.NotificationRepository
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import net.datafaker.Faker
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.lang.Thread.sleep
import java.util.Properties
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.missing-topics-fatal=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    ]
)
@EmbeddedKafka(
    partitions = 1,
    topics = ["notification.send", "notification.sent", "notification.failed", "notification.send.DLT"]
)
@ActiveProfiles("test")
class FullFlowIntegrationTest: BaseContainers() {

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @MockitoBean
    private lateinit var mailSender: JavaMailSender

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val faker = Faker()
    private val notificationsTopic = "notification.send"

    @BeforeEach
    fun setUpMailSender() {
        val session = Session.getInstance(Properties())
        whenever(mailSender.createMimeMessage()).thenReturn(MimeMessage(session))
    }

    @Test
    fun `should process notification event from kafka and update status correctly`() {
        val userId = faker.number().randomNumber(6, false)
        val userEmail = faker.internet().emailAddress()
        val eventId = faker.number().randomNumber(8, false)

        val notificationEvent = NotificationRequestedEvent(
            eventId = eventId,
            userId = userId,
            channel = Channel.EMAIL.name,
            payload = userEmail,
            occurredAt = java.time.Instant.now()
        )

        kafkaTemplate.send(notificationsTopic, userId.toString(), notificationEvent)
        sleep(2000)
        val notifications = notificationRepository.findByUserId(userId)
        Assertions.assertTrue(notifications.isNotEmpty())

        val notification = notifications.first()
        check(notification.status == Status.SENT)
        check(notification.channel == Channel.EMAIL)
        check(notification.payload == userEmail)
    }

    @Test
    fun `should handle invalid event and mark as failed`() {
        val invalidEvent = NotificationRequestedEvent(
            eventId = faker.number().randomNumber(8, false),
            userId = faker.number().randomNumber(6, false),
            channel = "INVALID_CHANNEL",
            payload = "invalid_payload",
            occurredAt = java.time.Instant.now()
        )

        kafkaTemplate.send(notificationsTopic, "invalid_key", invalidEvent)

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val failed = notificationRepository.findAll().filter { it.status == Status.FAILED }
            check(failed.isNotEmpty()) { "Не найдено уведомлений со статусом FAILED для невалидного события" }
        }

        verify(mailSender, never()).send(any<MimeMessage>())
    }
}
