package com.pitomets.monolit.model.entity

import com.pitomets.monolit.model.Gender
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "listing")
class Listing(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    var title: String,

    @Column(columnDefinition = "text")
    var description: String,

    var species: String? = null,
    var breed: String? = null,

    @Column(name = "age_months")
    var ageMonths: Int,

    @Enumerated(EnumType.STRING)
    var gender: Gender? = null,

    // mother/father reference to Pet (FK to pets.id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mother")
    var mother: Pet? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "father")
    var father: Pet? = null,

    var price: BigDecimal,

    @Column(name = "is_approved")
    var isApproved: Boolean = false,

    @Column(name = "manual_moderation_pending", nullable = false, columnDefinition = "boolean default false")
    var manualModerationPending: Boolean = false,

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

    @Column(name = "ai_model_version")
    var aiModelVersion: String? = null,

    @Column(name = "is_archived")
    var isArchived: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id")
    var sellerProfile: SellerProfile,

    @Column(name = "cover_photo_id")
    var coverPhotoId: Long? = null,

    @Column(name = "views_count")
    var viewsCount: Long = 0,

    @Column(name = "likes_count")
    var likesCount: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    var city: CityEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metro_station_id")
    var metroStation: MetroStationEntity? = null,
)
