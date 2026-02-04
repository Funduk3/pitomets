package com.pitomets.monolit.model.kafka

import com.pitomets.monolit.model.kafka.event.Channel
import java.time.Instant

data class NotificationRequestedEvent(
    val eventId: Long,
    val userId: Long,
    val channel: Channel,
    val payload: String,
    val timestamp: Instant = Instant.now()
)
