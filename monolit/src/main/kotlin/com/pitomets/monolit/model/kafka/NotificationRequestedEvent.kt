package com.pitomets.monolit.model.kafka

import com.pitomets.monolit.model.kafka.event.Channel

data class NotificationRequestedEvent(
    val eventId: Long,
    val userId: Long,
    val channel: Channel,
    val payload: String,
    val timestamp: java.time.Instant = java.time.Instant.now()
)
