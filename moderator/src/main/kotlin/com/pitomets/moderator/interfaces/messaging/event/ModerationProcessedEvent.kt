package com.pitomets.moderator.interfaces.messaging.event

import java.time.Instant
import java.util.UUID

data class ModerationProcessedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val requestEventId: UUID,
    val entityType: ModerationEntityType,
    val entityId: Long,
    val operation: ModerationOperation,
    val status: ModerationStatus,
    val reason: String? = null,
    val sourceAction: String? = null,
    val toxicityScore: Double? = null,
    val processingTimeMs: Long? = null,
    val modelVersion: String? = null,
    val processedAt: Instant = Instant.now()
)