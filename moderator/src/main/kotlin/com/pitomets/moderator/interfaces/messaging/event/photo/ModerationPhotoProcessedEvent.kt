package com.pitomets.moderator.interfaces.messaging.event.photo

import com.pitomets.moderator.interfaces.messaging.event.ModerationEntityType
import com.pitomets.moderator.interfaces.messaging.event.ModerationOperation
import com.pitomets.moderator.interfaces.messaging.event.ModerationStatus
import java.time.Instant
import java.util.UUID

data class ModerationPhotoProcessedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val requestEventId: UUID,
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