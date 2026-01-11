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
    fun execute(command: SendNotificationCommand) {
        logger.info("Processing notification for eventId: ${command.eventId}")

        if (notificationRepository.existsByEventId(command.eventId)) {
            logger.warn("Duplicate eventId: ${command.eventId}. Skipping.")
            return
        }

        val notification = Notification.create(command)
        logger.debug("Created notification: $notification")

        val savedNotification = notificationRepository.save(notification)
        logger.debug("Saved notification to repository with id=${savedNotification.id}")

        val sender = notificationSenders.find { it.channel() == command.channel }
            ?: throw IllegalArgumentException("No sender found for channel: ${command.channel}")

        try {
            sender.send(savedNotification)
            logger.info("Notification sent successfully: ${savedNotification.id}")

            // ВАЖНО: Используем markSent() и save() вместо updateStatus()
            val sentNotification = savedNotification.markSent()
            notificationRepository.save(sentNotification)
            logger.debug("Updated notification status to SENT")

            val event = NotificationSentEvent(
                notificationId = sentNotification.id!!,
                eventId = command.eventId,
                userId = command.userId,
                channel = command.channel
            )
            notificationOutbox.save(event)
            logger.debug("Saved NotificationSentEvent to outbox")
        } catch (e: FailedToSendNotificationException) {
            logger.error("Failed to send notification: ${savedNotification.id}", e)

            val failedNotification = savedNotification.markFailed()
            notificationRepository.save(failedNotification)

            val event = NotificationFailedEvent(
                notificationId = failedNotification.id ?: -1,
                eventId = command.eventId,
                userId = command.userId,
                channel = command.channel,
                error = e.message ?: "Unknown error"
            )
            notificationOutbox.save(event)

            logger.warn("Notification marked as FAILED for eventId: ${command.eventId}")
        }
    }
}
