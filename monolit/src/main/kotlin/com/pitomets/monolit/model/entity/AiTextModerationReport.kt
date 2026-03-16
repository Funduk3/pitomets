package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "ai_text_moderation_report",
    uniqueConstraints = [
        UniqueConstraint(
            name = "ux_ai_text_moderation_entity",
            columnNames = ["entity_id", "entity_type"]
        )
    ]
)
class AiTextModerationReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "entity_id")
    var entityId: Long,

    @Column(name = "entity_type")
    var entityType: String,

    @Column(name = "moderator_message", columnDefinition = "text")
    var moderatorMessage: String? = null,

    @Column(name = "ai_moderation_status")
    var aiModerationStatus: String? = null,

    @Column(name = "ai_moderation_reason", columnDefinition = "text")
    var aiModerationReason: String? = null,

    @Column(name = "ai_toxicity_score")
    var aiToxicityScore: Double? = null,

    @Column(name = "ai_profanity_detected")
    var aiProfanityDetected: Boolean? = null,

    @Column(name = "ai_sexual_content_detected")
    var aiSexualContentDetected: Boolean? = null,

    @Column(name = "ai_source_action")
    var aiSourceAction: String? = null,
)
