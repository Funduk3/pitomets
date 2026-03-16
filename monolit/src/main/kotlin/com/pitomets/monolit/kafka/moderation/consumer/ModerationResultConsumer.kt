package com.pitomets.monolit.kafka.moderation.consumer

import com.pitomets.monolit.model.AgeEnum
import com.pitomets.monolit.model.EventType
import com.pitomets.monolit.model.entity.AiPhotoModerationReport
import com.pitomets.monolit.model.entity.AiTextModerationReport
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.ListingOutbox
import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationProcessedEvent
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.repository.AiPhotoReportRepo
import com.pitomets.monolit.repository.AiTextReportRepo
import com.pitomets.monolit.repository.ListingOutboxRepository
import com.pitomets.monolit.repository.ListingPhotoRepo
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.ReviewsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.service.PhotoModerationUrlService
import com.pitomets.monolit.service.PhotoUrlService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ModerationResultConsumer(
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo,
    private val userRepo: UserRepo,
    private val reviewsRepo: ReviewsRepo,
    private val outboxRepo: ListingOutboxRepository,
    private val aiTextModerationRepo: AiTextReportRepo,
    private val aiPhotoModerationRepo: AiPhotoReportRepo,
    private val listingPhotoRepo: ListingPhotoRepo,
    private val photoUrlService: PhotoUrlService,
    private val photoModerationUrlService: PhotoModerationUrlService,
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
            val photosApproved = areListingPhotosApproved(requireNotNull(listing.id))
            val autoPublish = shouldAutoPublish(event) && photosApproved

            listing.isApproved = autoPublish
            if (autoPublish) {
                listing.manualModerationPending = false
            }

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

    @Suppress("ReturnCount")
    private fun areListingPhotosApproved(listingId: Long): Boolean {
        val photos = listingPhotoRepo.findByListingIdOrderByPosition(listingId)
        if (photos.isEmpty()) {
            return true
        }

        val photoUrls = photos.map { photoUrlService.objectUrl(it.objectKey) }
        val moderationUrls = photos.map { photoModerationUrlService.objectUrl(it.objectKey) }
        val photoKeys = photos.map { it.objectKey }
        val reports = aiPhotoModerationRepo.findByPhotoUriInAndEntityIdAndEntityType(
            (photoUrls + moderationUrls + photoKeys).distinct(),
            listingId,
            ModerationEntityType.LISTING.name
        )
        if (reports.isEmpty()) {
            return false
        }

        val reportsByUri = reports.groupBy { it.photoUri }
        return photos.all { photo ->
            val keys = listOf(
                photoUrlService.objectUrl(photo.objectKey),
                photoModerationUrlService.objectUrl(photo.objectKey),
                photo.objectKey
            )
            val matched = keys.flatMap { reportsByUri[it].orEmpty() }
            val worst = pickWorstPhotoReport(matched)
            worst?.aiModerationStatus == ModerationStatus.APPROVED.name
        }
    }

    private fun pickWorstPhotoReport(reports: List<AiPhotoModerationReport>): AiPhotoModerationReport? {
        if (reports.isEmpty()) {
            return null
        }
        val priority = mapOf(
            ModerationStatus.REJECTED.name to PRIORITY_REJECTED,
            ModerationStatus.REVIEW.name to PRIORITY_REVIEW,
            ModerationStatus.ERROR.name to PRIORITY_ERROR,
            ModerationStatus.APPROVED.name to PRIORITY_APPROVED
        )
        return reports.maxByOrNull { report ->
            priority[report.aiModerationStatus] ?: 0
        }
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
            log.warn("User {} not found for moderation result", event.entityId)
            return
        }
        val profile = sellerProfileRepo.findBySellerId(user.id ?: 0) ?: run {
            log.warn("Seller profile for user {} not found for moderation result", event.entityId)
            return
        }

        if (event.status == ModerationStatus.ERROR) {
            log.warn(
                "Moderation ERROR for seller user profile {}: {}",
                event.entityId,
                event.reason
            )
        }

        if (!profile.isApproved) {
            val wasApproved = profile.isApproved
            val autoPublish = shouldAutoPublish(event)

            profile.isApproved = autoPublish

            // Listing index updates apply only to listings.
        }

        saveAiTextModerationReport(event)

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

        if (review.manualModerationPending == true) {
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
        private const val PRIORITY_REJECTED = 4
        private const val PRIORITY_REVIEW = 3
        private const val PRIORITY_ERROR = 2
        private const val PRIORITY_APPROVED = 1
        private val log = LoggerFactory.getLogger(ModerationResultConsumer::class.java)
    }
}
