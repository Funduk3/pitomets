package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "reviews")
class Review(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "rating", nullable = false)
    var rating: Int,

    @Column(columnDefinition = "text")
    var text: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "is_approved")
    var isApproved: Boolean = false,

    @Column(name = "ai_moderation_status")
    var aiModerationStatus: String? = null,

    @Column(name = "ai_moderation_reason", columnDefinition = "text")
    var aiModerationReason: String? = null,

    @Column(name = "ai_toxicity_score")
    var aiToxicityScore: Double? = null,

    @Column(name = "ai_source_action")
    var aiSourceAction: String? = null,

    @Column(name = "ai_model_version")
    var aiModelVersion: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    var author: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    var sellerProfile: SellerProfile,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    var listing: Listing? = null,
)
