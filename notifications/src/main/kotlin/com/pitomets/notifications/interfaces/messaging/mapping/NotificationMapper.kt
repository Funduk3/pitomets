package com.pitomets.notifications.interfaces.messaging.mapping

import com.pitomets.notifications.application.command.SendNotificationCommand
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent

object NotificationMapper {
    fun toCommand(event: NotificationRequestedEvent): SendNotificationCommand {
        val channel = Channel.entries.firstOrNull { it.name.equals(event.channel, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unsupported channel: ${event.channel}")

        return SendNotificationCommand(
            eventId = event.eventId,
            userId = event.userId,
            channel = channel,
            payload = event.payload
        )
    }
}
