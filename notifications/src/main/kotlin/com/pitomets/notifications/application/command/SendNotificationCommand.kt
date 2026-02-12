package com.pitomets.notifications.application.command

import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.MessageType

data class SendNotificationCommand(
    val eventId: Long,
    val userId: Long,
    val channel: Channel,
    val messageType: MessageType,
    val payload: String
)
