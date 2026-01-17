package com.pitomets.monolit.model.kafka

import com.pitomets.monolit.model.kafka.event.Channel

data class NotificationSentEvent(
    val notificationId: Long,
    val eventId: Long,
    val userId: Long,
    val channel: Channel,
    val timestamp: java.time.Instant = java.time.Instant.now()
)