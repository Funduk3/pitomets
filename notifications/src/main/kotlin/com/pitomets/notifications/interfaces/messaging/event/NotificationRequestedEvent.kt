package com.pitomets.notifications.interfaces.messaging.event

import java.time.Instant

data class NotificationRequestedEvent(
    val eventId: Long,
    val userId: Long,
    val channel: String,
    val payload: String,
    val occurredAt: Instant = Instant.now()
)
