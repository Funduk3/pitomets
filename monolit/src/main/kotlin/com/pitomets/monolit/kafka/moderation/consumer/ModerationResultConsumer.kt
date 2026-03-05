package com.pitomets.monolit.kafka.moderation.consumer

import com.pitomets.monolit.model.AgeEnum
import com.pitomets.monolit.model.EventType
import com.pitomets.monolit.model.entity.ListingOutbox
import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationProcessedEvent
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.repository.ListingOutboxRepository
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
    private val reviewsRepo: ReviewsRepo,
    private val outboxRepo: ListingOutboxRepository
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

        if (event.status == ModerationStatus.ERROR) {
            log.warn(
                "Moderation ERROR for listing {}: {}",
                event.entityId,
                event.reason
            )
        }

        if (listing.manualModerationPending) {
            val wasApproved = listing.isApproved
            val autoPublish = shouldAutoPublish(event)

            listing.isApproved = autoPublish
            listing.moderatorMessage = null

            if (!wasApproved && autoPublish) {
                emitListingIndexUpsert(listing)
            } else if (wasApproved && !autoPublish) {
                emitListingIndexDelete(listing)
            }
        }

        applyAiResult(listing, event)
        listingsRepo.save(listing)
    }

    private fun shouldAutoPublish(event: ModerationProcessedEvent): Boolean {
        val toxicity = event.toxicityScore ?: return false
        return toxicity < LOW_TOXICITY_THRESHOLD &&
            event.profanityDetected != true &&
            event.sexualContentDetected != true
    }

    private fun emitListingIndexUpsert(listing: Listing) {
        outboxRepo.save(
            ListingOutbox(
                listingId = requireNotNull(listing.id),
                eventType = EventType.UPDATE,
                title = listing.title,
                description = listing.description,
                species = listing.species,
                breed = listing.breed,
                gender = listing.gender,
                ageEnum = AgeEnum.entries.getOrNull(listing.ageMonths)?.name,
                cityTitle = listing.city.title,
                city = listing.city.id,
                metro = listing.metroStation?.id,
                price = listing.price
            )
        )
    }

    private fun emitListingIndexDelete(listing: Listing) {
        outboxRepo.save(
            ListingOutbox(
                listingId = requireNotNull(listing.id),
                eventType = EventType.DELETE,
                title = null,
                description = null,
                city = 0,
                metro = null,
                price = 0.toBigDecimal()
            )
        )
    }

    private fun handleSellerProfile(event: ModerationProcessedEvent) {
        val profile = sellerProfileRepo.findById(event.entityId).orElse(null) ?: run {
            log.warn("Seller profile {} not found for moderation result", event.entityId)
            return
        }

        if (event.status == ModerationStatus.ERROR) {
            log.warn(
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

        if (event.status == ModerationStatus.ERROR) {
            log.warn("Moderation ERROR for review {}: {}", event.entityId, event.reason)
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
        listing.aiProfanityDetected = event.profanityDetected
        listing.aiSexualContentDetected = event.sexualContentDetected
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
        profile.aiProfanityDetected = event.profanityDetected
        profile.aiSexualContentDetected = event.sexualContentDetected
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
        review.aiProfanityDetected = event.profanityDetected
        review.aiSexualContentDetected = event.sexualContentDetected
        review.aiSourceAction = event.sourceAction
        review.aiModelVersion = event.modelVersion
    }

    companion object {
        private const val LOW_TOXICITY_THRESHOLD = 0.4
        private val log = LoggerFactory.getLogger(ModerationResultConsumer::class.java)
    }
}
