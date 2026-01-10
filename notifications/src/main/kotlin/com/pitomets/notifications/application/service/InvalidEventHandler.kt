package com.pitomets.notifications.application.service

import com.pitomets.notifications.application.event.NotificationFailedEvent
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.model.Status
import com.pitomets.notifications.domain.port.NotificationOutbox
import com.pitomets.notifications.domain.port.NotificationRepository
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InvalidEventHandler(
    private val notificationRepository: NotificationRepository,
    private val notificationOutbox: NotificationOutbox
) {
    private val logger = LoggerFactory.getLogger(InvalidEventHandler::class.java)

    @Transactional
    fun handle(event: NotificationRequestedEvent, error: Exception) {
        logger.info("Handling invalid event: ${event.eventId}")

        val failedNotification = Notification(
            id = null,
            eventId = event.eventId,
            userId = event.userId,
            channel = Channel.UNKNOWN,
            payload = event.payload,
            status = Status.FAILED
        )
        val saved = notificationRepository.save(failedNotification)
        logger.info("Saved failed notification with id: ${saved.id} for eventId: ${event.eventId}")

        notificationOutbox.save(
            NotificationFailedEvent(
                notificationId = saved.id ?: -1,
                eventId = event.eventId,
                userId = event.userId,
                channel = Channel.UNKNOWN,
                error = error.message ?: "Invalid event"
            )
        )
        logger.info("Saved failed event to outbox for notification: ${saved.id}")
    }
}
