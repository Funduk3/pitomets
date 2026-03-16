package com.pitomets.monolit.model.entity

import com.pitomets.monolit.model.entity.converter.StringListJsonConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "ai_photo_moderation_report",
    uniqueConstraints = [
        UniqueConstraint(
            name = "ux_ai_photo_moderation_entity",
            columnNames = ["entity_id", "entity_type", "photo_uri"]
        )
    ]
)
class AiPhotoModerationReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "entity_id")
    var entityId: Long,

    @Column(name = "entity_type")
    var entityType: String,

    @Column(name = "photo_uri", nullable = false)
    var photoUri: String,

    @Column(name = "ai_moderation_status")
    var aiModerationStatus: String ?= null,

    @Column(name = "ai_moderation_reason", columnDefinition = "text")
    var aiModerationReason: String? = null,

    @Column(name = "ai_labels", columnDefinition = "text")
    @Convert(converter = StringListJsonConverter::class)
    var aiLabels: List<String>? = null,

    @Column(name = "ai_toxicity_score")
    var aiToxicityScore: Double? = null,

    @Column(name = "ai_toxic_text_detected")
    var aiToxicTextDetected: Boolean? = null,

    @Column(name = "ai_toxic_text_matches", columnDefinition = "text")
    @Convert(converter = StringListJsonConverter::class)
    var aiToxicTextMatches: List<String>? = null,
)
