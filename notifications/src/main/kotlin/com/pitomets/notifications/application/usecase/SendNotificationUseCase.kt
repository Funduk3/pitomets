package com.pitomets.notifications.application.usecase

import com.pitomets.notifications.application.command.SendNotificationCommand
import com.pitomets.notifications.application.event.NotificationFailedEvent
import com.pitomets.notifications.application.event.NotificationSentEvent
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.port.NotificationOutbox
import com.pitomets.notifications.domain.port.NotificationRepository
import com.pitomets.notifications.domain.port.NotificationSender
import com.pitomets.notifications.exceptions.FailedToSendNotificationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class SendNotificationUseCase(
    private val notificationRepository: NotificationRepository,
    private val notificationOutbox: NotificationOutbox,
    private val notificationSenders: List<NotificationSender>
) {

    private val logger = LoggerFactory.getLogger(SendNotificationUseCase::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Suppress("TooGenericExceptionCaught")
    fun execute(command: SendNotificationCommand) {
        logger.info("Processing notification for eventId: ${command.eventId}")

        // Проверка идемпотентности - если уже обработали, просто выходим
        if (notificationRepository.existsByEventId(command.eventId)) {
            logger.info("Notification already processed for eventId: ${command.eventId}. Skipping.")
            return // Успешное завершение - офсет будет закоммичен
        }

        val notification = Notification.create(command)
        logger.debug("Created notification: {}", notification)

        val savedNotification = try {
            notificationRepository.save(notification)
        } catch (e: DataIntegrityViolationException) {
            // Race condition - другой процесс уже вставил уведомление с тем же eventId
            logger.info("Race condition detected for eventId=${command.eventId}. Another process already saved it.")
            return // Успешное завершение - сообщение обработано
        } catch (e: DataAccessException) {
            logger.error("Database access error while saving notification for eventId=${command.eventId}", e)
            throw e
        }

        logger.debug("Saved notification to repository with id={}", savedNotification.id)

        val sender = notificationSenders.find { it.channel() == command.channel }
            ?: throw IllegalArgumentException("No sender found for channel: ${command.channel}")

        try {
            sender.send(savedNotification)
            logger.info("Notification sent successfully: ${savedNotification.id}")

            // Используем markSent() и save() вместо updateStatus()
            val sentNotification = savedNotification.markSent()
            notificationRepository.save(sentNotification)
            logger.debug("Updated notification status to SENT")

            val event = NotificationSentEvent(
                notificationId = sentNotification.id!!,
                eventId = command.eventId,
                userId = command.userId,
                channel = command.channel
            )
            saveOutboxOrThrow(event, "NotificationSentEvent", command.eventId)
            logger.debug("Saved NotificationSentEvent to outbox")

        } catch (e: FailedToSendNotificationException) {
            logger.error("Failed to send notification: ${savedNotification.id}", e)
            handleFailedNotification(savedNotification, command, e.message ?: "Unknown error")
            // НЕ пробрасываем исключение - обработка завершена, состояние сохранено

        } catch (e: Exception) {
            logger.error("Unexpected error while sending notification: ${savedNotification.id}", e)
            handleFailedNotification(savedNotification, command, e.message ?: "Unknown error")
            // НЕ пробрасываем исключение - обработка завершена, состояние сохранено
        }
    }

    private fun handleFailedNotification(
        notification: Notification,
        command: SendNotificationCommand,
        errorMessage: String
    ) {
        val failedNotification = notification.markFailed()
        notificationRepository.save(failedNotification)

        val event = NotificationFailedEvent(
            notificationId = failedNotification.id ?: -1,
            eventId = command.eventId,
            userId = command.userId,
            channel = command.channel,
            error = errorMessage
        )
        saveOutboxOrThrow(event, "NotificationFailedEvent", command.eventId)

        logger.warn("Notification marked as FAILED for eventId: ${command.eventId}")
    }

    private fun saveOutboxOrThrow(event: Any, eventName: String, eventId: Long) {
        try {
            notificationOutbox.save(event)
        } catch (e: DataAccessException) {
            logger.error("Database access error while saving {} for eventId={}", eventName, eventId, e)
            throw e
        }
    }
}
