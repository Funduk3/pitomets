package com.pitomets.moderator.interfaces.messaging.event

import java.time.Instant
import java.util.UUID

data class ModerationRequestedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val entityType: ModerationEntityType,
    val entityId: Long,
    val operation: ModerationOperation,
    val textParts: List<String> = emptyList(),
    val withAnimal: Boolean? = null,
    val requestedAt: Instant = Instant.now()
)
