package com.pitomets.notifications.interfaces.messaging.mapping

import com.pitomets.notifications.application.command.SendNotificationCommand
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent

object NotificationMapper {
    fun toCommand(event: NotificationRequestedEvent) =
        SendNotificationCommand(
            eventId = event.eventId,
            userId = event.userId,
            channel = Channel.valueOf(event.channel),
            payload = event.payload
        )
}
