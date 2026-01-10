package com.pitomets.notifications.application.event

data class NotificationFailedEvent(
    val notificationId: Long,
    val eventId: Long,
    val userId: Long,
    val channel: com.pitomets.notifications.domain.model.Channel,
    val error: String,
    val timestamp: java.time.Instant = java.time.Instant.now()
)
