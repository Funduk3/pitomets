package com.pitomets.monolit.model.kafka.moderation

import java.time.Instant
import java.util.UUID

enum class ModerationEntityType {
    LISTING,
    USER,
    REVIEW
}

enum class ModerationOperation {
    CREATE,
    UPDATE
}

enum class ModerationStatus {
    APPROVED,
    REJECTED,
    REVIEW,
    ERROR
}

data class ModerationRequestedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val entityType: ModerationEntityType,
    val entityId: Long,
    val operation: ModerationOperation,
    val textParts: List<String> = emptyList(),
    val withAnimal: Boolean? = null,
    val requestedAt: Instant = Instant.now()
)

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
    val profanityDetected: Boolean? = null,
    val sexualContentDetected: Boolean? = null,
    val processingTimeMs: Long? = null,
    val modelVersion: String? = null,
    val processedAt: Instant = Instant.now()
)
