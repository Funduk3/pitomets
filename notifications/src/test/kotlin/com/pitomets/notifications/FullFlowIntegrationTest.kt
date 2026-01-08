package com.pitomets.notifications

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.Status
import com.pitomets.notifications.domain.port.NotificationRepository
import jakarta.mail.internet.MimeMessage
import net.datafaker.Faker
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.yml"])
@DirtiesContext
class FullFlowIntegrationTest : BaseTest() {  // <- Изменили здесь

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    private lateinit var mailSender: JavaMailSender

    @Value("\${kafka.topics.notifications}")
    private lateinit var notificationsTopic: String

    private val faker = Faker()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should process notification event from kafka and update status correctly`() {
        // Ваш существующий тест - без изменений
        val userId = faker.number().randomNumber(6, false).toString().toLong()
        val userEmail = faker.internet().emailAddress()
        val eventId = faker.number().randomNumber(8, false).toString().toLong()

        val notificationEvent = mapOf(
            "eventId" to eventId,
            "userId" to userId,
            "channel" to Channel.EMAIL.name,
            "payload" to """
                {
                    "subject": "Order Confirmation",
                    "body": "Your order #${faker.number().digits(6)} has been confirmed",
                    "recipient": "$userEmail"
                }
            """.trimIndent()
        )

        kafkaTemplate.send(
            ProducerRecord(notificationsTopic, userId.toString(), mapper.writeValueAsString(notificationEvent))
        )
        println("✅ Отправлено тестовое событие в Kafka: $notificationEvent")

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val notifications = notificationRepository.findByUserId(userId)
            assert(notifications.isNotEmpty()) {
                "Уведомление не сохранено в БД для пользователя $userId"
            }

            val notification = notifications.first()
            assert(notification?.status == Status.NEW) {
                "Ожидался статус NEW, получен ${notification?.status}"
            }
            assert(notification?.channel == Channel.EMAIL) {
                "Неверный канал: ${notification?.channel}"
            }
            assert(notification?.payload?.contains(userEmail) == true) {
                "Payload не содержит email получателя"
            }
            println("✅ Уведомление сохранено в БД со статусом NEW: $notification")
        }

        await.atMost(15, TimeUnit.SECONDS).untilAsserted {
            verify(mailSender).send(any<MimeMessage>())
            println("✅ Подтверждена отправка email на $userEmail")
        }

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val updatedNotification = notificationRepository.findByEventId(eventId)

            assert(updatedNotification?.status == Status.SENT) {
                "Статус не обновлен на SENT, текущий статус: ${updatedNotification?.status}"
            }
            println("✅ Статус уведомления успешно обновлен на SENT")
        }

        println("🎉 Тест пройден успешно: обработка Kafka → сохранение → отправка → обновление статуса")
    }

    @Test
    fun `should handle invalid event and mark as failed`() {
        // Ваш существующий тест
        val invalidEvent = """
            {
                "userId": "${faker.number().randomNumber(6, false)}",
                "channel": "INVALID_CHANNEL",
                "payload": "invalid"
            }
        """.trimIndent()

        kafkaTemplate.send(notificationsTopic, "invalid_key", invalidEvent)

        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val notifications = notificationRepository.findAll()
                .filter { it?.status == Status.FAILED }

            assert(notifications.isNotEmpty()) {
                "Не найдено уведомлений со статусом FAILED для невалидного события"
            }
            println("✅ Невалидное событие обработано и помечено как FAILED")
        }

        Thread.sleep(2000)
        verify(mailSender, never()).send(any<MimeMessage>())
        println("✅ Подтверждено, что email не был отправлен для невалидного события")
    }
}