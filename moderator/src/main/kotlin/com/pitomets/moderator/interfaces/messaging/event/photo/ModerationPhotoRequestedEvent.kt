package com.pitomets.moderator.interfaces.messaging.event.photo

import com.pitomets.moderator.interfaces.messaging.event.ModerationEntityType
import java.util.UUID
import java.time.Instant

data class ModerationPhotoRequestedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val entityType: ModerationEntityType,
    val entityId: Long,
    val photoURI: String,
    val requestedAt: Instant = Instant.now(),
)
