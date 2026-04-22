package com.pitomets.notifications

import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.Status
import com.pitomets.notifications.domain.port.NotificationRepository
import com.pitomets.notifications.infrastructure.outbox.OutboxJpaRepository
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import net.datafaker.Faker
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.Properties
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.missing-topics-fatal=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.frontend-url=http://test-localhost:3000"
    ]
)
@EmbeddedKafka(
    partitions = 1,
    topics = ["notification.send", "notification.sent", "notification.failed", "notification.send.DLT"]
)
@ActiveProfiles("test")
class FullFlowIntegrationTest : BaseContainers() {

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var outboxJpaRepository: OutboxJpaRepository

    @MockitoBean
    private lateinit var mailSender: JavaMailSender

    private val faker = Faker()
    private val notificationsTopic = "notification.send"

    @BeforeEach
    fun setUpMailSender() {
        val session = Session.getInstance(Properties())
        whenever(mailSender.createMimeMessage()).thenReturn(MimeMessage(session))
    }

    @Test
    fun `should process notification event, generate correct link and send email`() {
        val userId = faker.number().randomNumber(6, false)
        val userEmail = faker.internet().emailAddress()
        val token = "my-secret-token-123"
        val fullPayload = "$userEmail $token"
        val eventId = faker.number().randomNumber(8, false)

        val notificationEvent = NotificationRequestedEvent(
            eventId = eventId,
            userId = userId,
            channel = Channel.EMAIL.name,
            messageType = "RESTORE_PASSWORD",
            payload = fullPayload,
            occurredAt = Instant.now()
        )

        kafkaTemplate.send(notificationsTopic, userId.toString(), notificationEvent)

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val notifications = notificationRepository.findByUserId(userId)
            assertTrue(notifications.isNotEmpty())
            val notification = notifications.first()
            assertEquals(Status.SENT, notification.status)
            assertEquals(fullPayload, notification.payload)
        }

        val messageCaptor = argumentCaptor<MimeMessage>()
        verify(mailSender, times(1)).send(messageCaptor.capture())
    }

    @Test
    fun `should handle mail server error and mark notification as FAILED`() {
        val userId = faker.number().randomNumber(6, false)
        val eventId = faker.number().randomNumber(8, false)

        whenever(mailSender.send(any<MimeMessage>())).thenThrow(MailSendException("SMTP error"))

        val notificationEvent = NotificationRequestedEvent(
            eventId = eventId,
            userId = userId,
            channel = Channel.EMAIL.name,
            payload = "test@example.com",
            messageType = "REGISTRATION",
            occurredAt = Instant.now()
        )

        kafkaTemplate.send(notificationsTopic, userId.toString(), notificationEvent)

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val notifications = notificationRepository.findByUserId(userId)
            assertTrue(notifications.isNotEmpty())

            val notification = notifications.first()
            assertEquals(Status.FAILED, notification.status, "Статус должен быть FAILED при ошибке SMTP")
        }
    }

    @Test
    fun `should handle invalid event (bad channel) and mark as failed without sending mail`() {
        val invalidEvent = NotificationRequestedEvent(
            eventId = faker.number().randomNumber(8, false),
            userId = faker.number().randomNumber(6, false),
            channel = "SMS",
            messageType = "REGISTRATION",
            payload = "invalid_payload",
            occurredAt = Instant.now()
        )

        kafkaTemplate.send(notificationsTopic, "invalid_key", invalidEvent)

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val failed = notificationRepository.findAll()
                .filter { it.userId == invalidEvent.userId && it.status == Status.FAILED }
            assertTrue(failed.isNotEmpty(), "Не найдено уведомлений со статусом FAILED")
        }

        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun `should process duplicate event id only once`() {
        val userId = faker.number().randomNumber(6, false)
        val eventId = faker.number().randomNumber(8, false)
        val payload = "${faker.internet().emailAddress()} token-${faker.number().digits(6)}"

        val event = NotificationRequestedEvent(
            eventId = eventId,
            userId = userId,
            channel = Channel.EMAIL.name,
            messageType = "CONFIRM",
            payload = payload,
            occurredAt = Instant.now()
        )

        kafkaTemplate.send(notificationsTopic, userId.toString(), event)
        kafkaTemplate.send(notificationsTopic, userId.toString(), event)

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val stored = notificationRepository.findAll().filter { it.eventId == eventId }
            assertEquals(1, stored.size, "Должна быть только одна запись для одного eventId")
            assertEquals(Status.SENT, stored.first().status)
        }

        verify(mailSender, times(1)).send(any<MimeMessage>())
    }

    @Test
    fun `should create outbox event after successful processing`() {
        val userId = faker.number().randomNumber(6, false)
        val eventId = faker.number().randomNumber(8, false)
        val payload = "${faker.internet().emailAddress()} token-${faker.number().digits(6)}"

        val event = NotificationRequestedEvent(
            eventId = eventId,
            userId = userId,
            channel = Channel.EMAIL.name,
            messageType = "CONFIRM",
            payload = payload,
            occurredAt = Instant.now()
        )

        kafkaTemplate.send(notificationsTopic, userId.toString(), event)

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val outboxEvents = outboxJpaRepository.findAll()
            assertTrue(
                outboxEvents.any { it.eventType.contains("NotificationSentEvent") && !it.published },
                "Ожидали запись NotificationSentEvent в outbox"
            )
        }
    }
}
