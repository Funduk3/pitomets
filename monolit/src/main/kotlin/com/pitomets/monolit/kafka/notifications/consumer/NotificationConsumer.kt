package com.pitomets.monolit.kafka.notifications.consumer

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class NotificationConsumer {

    private val logger = LoggerFactory.getLogger(NotificationConsumer::class.java)

    @KafkaListener(
        topics = [
            "\${notification.kafka.topics.notification-sent}",
            "\${notification.kafka.topics.notification-failed}"
        ],
        groupId = "user-service-notification-status"
    )
    fun consumeNotificationStatus(message: String) {
        try {
            logger.info("Received notification status: $message")

            // Здесь можно добавить логику обработки статусов
            // например, обновление статуса в базе данных

            // cюда добавить NotificationFailedEvent
        } catch (e: IllegalStateException) {
            logger.error("Error processing notification status", e)
            throw e
        }
    }
}
