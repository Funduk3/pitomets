package com.pitomets.moderator.interfaces.messaging.event.photo

import java.util.UUID
import java.time.Instant

data class ModerationPhotoRequestedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val photoURI: String,
    val requestedAt: Instant = Instant.now(),
)