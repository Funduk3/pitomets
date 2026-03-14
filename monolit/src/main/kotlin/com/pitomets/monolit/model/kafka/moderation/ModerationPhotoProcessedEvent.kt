package com.pitomets.monolit.model.kafka.moderation

import com.pitomets.monolit.model.EventType
import java.time.Instant
import java.util.UUID

data class ModerationPhotoProcessedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val requestEventId: UUID,
    val entityType: ModerationEntityType,
    val entityId: Long,
    val photoURI: String,
    val status: ModerationStatus,
    val reason: String? = null,
    val sourceAction: String? = null,
    val toxicityScore: Double? = null,
    val labels: List<String>? = null,
    val detectedText: Boolean? = null,
    val preview: List<String>? = null,
    val toxicTextDetected: Boolean? = null,
    val toxicMatches: List<String>? = null,
    val processingTimeMs: Long? = null,
    val modelVersion: String? = null,
    val processedAt: Instant = Instant.now()
)
