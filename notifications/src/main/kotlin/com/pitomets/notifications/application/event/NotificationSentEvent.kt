package com.pitomets.notifications.application.event

data class NotificationSentEvent(
    val notificationId: Long,
    val eventId: Long,
    val userId: Long,
    val channel: com.pitomets.notifications.domain.model.Channel,
    val timestamp: java.time.Instant = java.time.Instant.now()
)
