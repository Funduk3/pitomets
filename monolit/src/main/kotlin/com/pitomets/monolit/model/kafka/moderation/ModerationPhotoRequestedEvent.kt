package com.pitomets.monolit.model.kafka.moderation

import java.util.UUID
import java.time.Instant

data class ModerationPhotoRequestedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val entityType: ModerationEntityType,
    val entityId: Long,
    val photoURI: String,
    val requestedAt: Instant = Instant.now(),
)
