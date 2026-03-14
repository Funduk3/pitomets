package com.pitomets.monolit.kafka.moderation.consumer

import com.pitomets.monolit.model.AgeEnum
import com.pitomets.monolit.model.EventType
import com.pitomets.monolit.model.entity.AiTextModerationReport
import com.pitomets.monolit.model.entity.ListingOutbox
import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationProcessedEvent
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.repository.AiTextReportRepo
import com.pitomets.monolit.repository.ListingOutboxRepository
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.ReviewsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ModerationResultConsumer(
    private val listingsRepo: ListingsRepo,
    private val userRepo: UserRepo,
    private val reviewsRepo: ReviewsRepo,
    private val outboxRepo: ListingOutboxRepository,
    private val aiTextModerationRepo: AiTextReportRepo
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
            ModerationEntityType.USER -> handleSellerProfile(event)
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

            if (!wasApproved && autoPublish) {
                emitListingIndexUpsert(listing)
            } else if (wasApproved && !autoPublish) {
                emitListingIndexDelete(listing)
            }
        }

        saveAiTextModerationReport(event)

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
        val user = userRepo.findById(event.entityId).orElse(null) ?: run {
            log.warn("Seller profile {} not found for moderation result", event.entityId)
            return
        }

        if (event.status == ModerationStatus.ERROR) {
            log.warn(
                "Moderation ERROR for seller user profile {}: {}",
                event.entityId,
                event.reason
            )
        }

        if (user.manualModerationPending) {
            val wasApproved = user.isApproved
            val autoPublish = shouldAutoPublish(event)

            user.isApproved = autoPublish

            // Listing index updates apply only to listings.
        }

        saveAiTextModerationReport(event)

        userRepo.save(user)
    }

    private fun handleReview(event: ModerationProcessedEvent) {
        val review = reviewsRepo.findById(event.entityId).orElse(null) ?: run {
            log.warn("Review {} not found for moderation result", event.entityId)
            return
        }

        if (event.status == ModerationStatus.ERROR) {
            log.warn("Moderation ERROR for review {}: {}", event.entityId, event.reason)
        }

        if (review.manualModerationPending) {
            val wasApproved = review.isApproved
            val autoPublish = shouldAutoPublish(event)

            review.isApproved = autoPublish

            // Listing index updates apply only to listings.
        }

        saveAiTextModerationReport(event)

        reviewsRepo.save(review)
    }

    private fun saveAiTextModerationReport(event: ModerationProcessedEvent) {
        val report = aiTextModerationRepo.findByEntityIdAndEntityType(
            event.entityId,
            event.entityType.name
        ) ?: AiTextModerationReport(
            entityId = event.entityId,
            entityType = event.entityType.name
        )

        report.aiModerationStatus = event.status.name
        report.aiModerationReason = event.reason
        report.aiToxicityScore = event.toxicityScore
        report.aiProfanityDetected = event.profanityDetected
        report.aiSexualContentDetected = event.sexualContentDetected
        report.aiSourceAction = event.sourceAction

        aiTextModerationRepo.save(report)
    }

    companion object {
        private const val LOW_TOXICITY_THRESHOLD = 0.4
        private val log = LoggerFactory.getLogger(ModerationResultConsumer::class.java)
    }
}
