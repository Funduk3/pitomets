package com.pitomets.notifications.application.usecase

import com.pitomets.notifications.application.command.SendNotificationCommand
import com.pitomets.notifications.application.event.NotificationFailedEvent
import com.pitomets.notifications.application.event.NotificationSentEvent
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.port.NotificationOutbox
import com.pitomets.notifications.domain.port.NotificationRepository
import com.pitomets.notifications.domain.port.NotificationSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SendNotificationUseCase(
    private val notificationRepository: NotificationRepository,
    private val notificationOutbox: NotificationOutbox,
    private val notificationSenders: List<NotificationSender>
) {

    private val logger = LoggerFactory.getLogger(SendNotificationUseCase::class.java)

    @Transactional
    fun execute(command: SendNotificationCommand) {
        logger.info("Processing notification for eventId: ${command.eventId}")

        if (notificationRepository.existsByEventId(command.eventId)) {
            logger.warn("Duplicate eventId: ${command.eventId}. Skipping.")
            return
        }

        val notification = Notification.create(command)
        logger.debug("Created notification: $notification")

        notificationRepository.save(notification)
        logger.debug("Saved notification to repository")

        val sender = notificationSenders.find { it.channel() == command.channel }
            ?: throw IllegalArgumentException("No sender found for channel: ${command.channel}")

        try {
            sender.send(notification)
            logger.info("Notification sent successfully: ${notification.id}")

            val event = NotificationSentEvent(
                notificationId = notification.id,
                eventId = command.eventId,
                userId = command.userId,
                channel = command.channel
            )
            notificationOutbox.save(event)
            logger.debug("Saved NotificationSentEvent to outbox")

        } catch (e: Exception) {
            logger.error("Failed to send notification: ${notification.id}", e)

            val event = NotificationFailedEvent(
                notificationId = notification.id,
                eventId = command.eventId,
                userId = command.userId,
                channel = command.channel,
                error = e.message ?: "Unknown error"
            )
            notificationOutbox.save(event)

            throw e
        }
    }
}
