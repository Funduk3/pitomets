package com.pitomets.monolit.kafka.moderation.consumer

import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationProcessedEvent
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.ReviewsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ModerationResultConsumer(
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo,
    private val reviewsRepo: ReviewsRepo
) {
    @KafkaListener(
        topics = ["\${moderation.kafka.topics.moderation-processed}"],
        containerFactory = "moderationKafkaListenerContainerFactory"
    )
    @Transactional
    fun consume(event: ModerationProcessedEvent) {
        log.info(
            "Received moderation result eventId={} entityType={} entityId={} status={}",
            event.eventId,
            event.entityType,
            event.entityId,
            event.status
        )

        when (event.entityType) {
            ModerationEntityType.LISTING -> handleListing(event)
            ModerationEntityType.SELLER_PROFILE -> handleSellerProfile(event)
            ModerationEntityType.REVIEW -> handleReview(event)
        }
    }

    private fun handleListing(event: ModerationProcessedEvent) {
        val listing = listingsRepo.findById(event.entityId).orElse(null) ?: run {
            log.warn("Listing {} not found for moderation result", event.entityId)
            return
        }

        when (event.status) {
            ModerationStatus.APPROVED -> {
                listing.isApproved = true
                listing.moderatorMessage = null
            }

            ModerationStatus.REJECTED, ModerationStatus.REVIEW -> {
                listing.isApproved = false
                listing.moderatorMessage = event.reason ?: "Объявление отклонено автоматической модерацией"
            }

            ModerationStatus.ERROR -> log.warn(
                "Moderation ERROR for listing {}: {}",
                event.entityId,
                event.reason
            )
        }

        applyAiResult(listing, event)
        listingsRepo.save(listing)
    }

    private fun handleSellerProfile(event: ModerationProcessedEvent) {
        val profile = sellerProfileRepo.findById(event.entityId).orElse(null) ?: run {
            log.warn("Seller profile {} not found for moderation result", event.entityId)
            return
        }

        when (event.status) {
            ModerationStatus.APPROVED -> {
                profile.isApproved = true
                profile.isVerified = true
            }

            ModerationStatus.REJECTED, ModerationStatus.REVIEW -> {
                profile.isApproved = false
                profile.isVerified = false
            }

            ModerationStatus.ERROR -> log.warn(
                "Moderation ERROR for seller profile {}: {}",
                event.entityId,
                event.reason
            )
        }

        applyAiResult(profile, event)
        sellerProfileRepo.save(profile)
    }

    private fun handleReview(event: ModerationProcessedEvent) {
        val review = reviewsRepo.findById(event.entityId).orElse(null) ?: run {
            log.warn("Review {} not found for moderation result", event.entityId)
            return
        }

        when (event.status) {
            ModerationStatus.APPROVED -> {
                review.isApproved = true
            }

            ModerationStatus.REJECTED, ModerationStatus.REVIEW -> {
                review.isApproved = false
            }

            ModerationStatus.ERROR -> {
                review.isApproved = false
                log.warn("Moderation ERROR for review {}: {}", event.entityId, event.reason)
            }
        }

        applyAiResult(review, event)
        reviewsRepo.save(review)
    }

    private fun applyAiResult(
        listing: Listing,
        event: ModerationProcessedEvent
    ) {
        listing.aiModerationStatus = event.status.name
        listing.aiModerationReason = event.reason
        listing.aiToxicityScore = event.toxicityScore
        listing.aiSourceAction = event.sourceAction
        listing.aiModelVersion = event.modelVersion
    }

    private fun applyAiResult(
        profile: SellerProfile,
        event: ModerationProcessedEvent
    ) {
        profile.aiModerationStatus = event.status.name
        profile.aiModerationReason = event.reason
        profile.aiToxicityScore = event.toxicityScore
        profile.aiSourceAction = event.sourceAction
        profile.aiModelVersion = event.modelVersion
    }

    private fun applyAiResult(
        review: Review,
        event: ModerationProcessedEvent
    ) {
        review.aiModerationStatus = event.status.name
        review.aiModerationReason = event.reason
        review.aiToxicityScore = event.toxicityScore
        review.aiSourceAction = event.sourceAction
        review.aiModelVersion = event.modelVersion
    }

    companion object {
        private val log = LoggerFactory.getLogger(ModerationResultConsumer::class.java)
    }
}
