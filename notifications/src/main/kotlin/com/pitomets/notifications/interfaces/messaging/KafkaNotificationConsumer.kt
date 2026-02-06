package com.pitomets.notifications.interfaces.messaging

import com.pitomets.notifications.application.service.InvalidEventHandler
import com.pitomets.notifications.application.usecase.SendNotificationUseCase
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent
import com.pitomets.notifications.interfaces.messaging.mapping.NotificationMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaNotificationConsumer(
    private val useCase: SendNotificationUseCase,
    private val invalidEventHandler: InvalidEventHandler
) {
    private val logger = LoggerFactory.getLogger(KafkaNotificationConsumer::class.java)

    @KafkaListener(
        topics = ["notification.send"],
        groupId = "notification-service"
    )
    @Suppress("TooGenericExceptionCaught")
    fun consume(event: NotificationRequestedEvent) {
        try {
            val command = NotificationMapper.toCommand(event)
            useCase.execute(command)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Invalid notification request ${event.eventId}: ${ex.message}")
            invalidEventHandler.handle(event, ex)
        } catch (ex: Exception) {
            // Перехватываем любые неожиданные ошибки, чтобы не допускать автоматического ретрая контейнера
            logger.error("Unexpected error while processing notification ${event.eventId}: ${ex.message}", ex)
            try {
                invalidEventHandler.handle(event, ex)
            } catch (handlerEx: Exception) {
                logger.error("Failed to handle invalid event ${event.eventId}", handlerEx)
            }
            // не пробрасываем исключение дальше — обработали и сохранили ошибку
        }
    }
}
