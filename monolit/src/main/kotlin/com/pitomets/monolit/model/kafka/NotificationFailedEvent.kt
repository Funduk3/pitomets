package com.pitomets.monolit.model.kafka

data class NotificationFailedEvent(
    val notificationId: Long,
    val eventId: Long,
    val userId: Long,
    val channel: String,
    val error: String
)
