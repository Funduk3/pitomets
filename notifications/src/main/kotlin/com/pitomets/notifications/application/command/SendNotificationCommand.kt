package com.pitomets.notifications.application.command

import com.pitomets.notifications.domain.model.Channel

data class SendNotificationCommand(
    val eventId: Long,
    val userId: Long,
    val channel: Channel,
    val payload: String
)
