package com.pitomets.monolit.kafka.moderation.consumer

import com.pitomets.monolit.model.AgeEnum
import com.pitomets.monolit.model.EventType
import com.pitomets.monolit.model.entity.AiPhotoModerationReport
import com.pitomets.monolit.model.entity.ListingOutbox
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationPhotoProcessedEvent
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.repository.AiTextReportRepo
import com.pitomets.monolit.repository.AiPhotoReportRepo
import com.pitomets.monolit.repository.ListingOutboxRepository
import com.pitomets.monolit.repository.ListingPhotoRepo
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.service.PhotoModerationUrlService
import com.pitomets.monolit.service.PhotoUrlService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ModerationPhotoConsumer(
    private val listingsRepo: ListingsRepo,
    private val userRepo: UserRepo,
    private val aiPhotoModerationReport: AiPhotoReportRepo,
    private val aiTextReportRepo: AiTextReportRepo,
    private val listingPhotoRepo: ListingPhotoRepo,
    private val listingOutboxRepo: ListingOutboxRepository,
    private val photoUrlService: PhotoUrlService,
    private val photoModerationUrlService: PhotoModerationUrlService,
) {
    @KafkaListener(
        topics = ["\${moderation.kafka.topics.moderation-processed-photo}"],
        containerFactory = "moderationKafkaListenerContainerFactory"
    )
    fun consume(event: ModerationPhotoProcessedEvent) {
        log.info(
            "Received moderation photo result for {}, status={}",
            event.eventId,
            event.status
        )

        when (event.entityType) {
            ModerationEntityType.LISTING -> handleListing(event)
            ModerationEntityType.USER -> handleSellerProfile(event)
            else -> log.warn("Photo can be only in Listings or Seller profiles")
        }
    }

    private fun handleListing(event: ModerationPhotoProcessedEvent) {
        val listing = listingsRepo.findById(event.entityId).orElse(null) ?: run {
            log.warn("Listing {} not found for photo moderation result", event.entityId)
            return
        }

        if (event.status == ModerationStatus.ERROR) {
            log.warn(
                "Photo moderation ERROR for listing {}: {}",
                event.entityId,
                event.reason
            )
        }

        saveAiPhotoModerationReport(event)

        if (listing.manualModerationPending) {
            val wasApproved = listing.isApproved
            val photosApproved = areListingPhotosApproved(requireNotNull(listing.id))
            val textApproved = isTextApproved(requireNotNull(listing.id))
            val autoPublish = photosApproved && textApproved

            listing.isApproved = autoPublish
            if (autoPublish) {
                listing.manualModerationPending = false
            }

            if (!wasApproved && autoPublish) {
                emitListingIndexUpsert(listing)
            } else if (wasApproved && !autoPublish) {
                emitListingIndexDelete(listing)
            }

            listingsRepo.save(listing)
        }
    }

    private fun handleSellerProfile(event: ModerationPhotoProcessedEvent) {
        val user = userRepo.findById(event.entityId).orElse(null) ?: run {
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

        saveAiPhotoModerationReport(event)
    }

    private fun saveAiPhotoModerationReport(event: ModerationPhotoProcessedEvent) {
        val report = aiPhotoModerationReport.findByPhotoUriAndEntityIdAndEntityType(
            event.photoURI,
            event.entityId,
            event.entityType.name
        )
            ?: AiPhotoModerationReport(
                entityId = event.entityId,
                entityType = event.entityType.name,
                photoUri = event.photoURI
            )

        report.entityId = event.entityId
        report.entityType = event.entityType.name
        report.photoUri = event.photoURI
        report.aiModerationReason = event.reason
        report.aiLabels = event.labels
        report.aiToxicityScore = event.toxicityScore
        report.aiModerationStatus = event.status.name
        report.aiToxicTextMatches = event.toxicMatches
        report.aiToxicTextDetected = event.toxicTextDetected

        aiPhotoModerationReport.save(report)
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
        val reports = aiPhotoModerationReport.findByPhotoUriInAndEntityIdAndEntityType(
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

    @Suppress("ReturnCount")
    private fun isTextApproved(listingId: Long): Boolean {
        val report = aiTextReportRepo.findByEntityIdAndEntityType(
            listingId,
            ModerationEntityType.LISTING.name
        ) ?: return false

        val toxicity = report.aiToxicityScore ?: return false
        return toxicity < LOW_TOXICITY_THRESHOLD &&
            report.aiProfanityDetected != true &&
            report.aiSexualContentDetected != true
    }

    private fun emitListingIndexUpsert(listing: com.pitomets.monolit.model.entity.Listing) {
        listingOutboxRepo.save(
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

    private fun emitListingIndexDelete(listing: com.pitomets.monolit.model.entity.Listing) {
        listingOutboxRepo.save(
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


    companion object {
        private const val LOW_TOXICITY_THRESHOLD = 0.4
        private const val PRIORITY_REJECTED = 4
        private const val PRIORITY_REVIEW = 3
        private const val PRIORITY_ERROR = 2
        private const val PRIORITY_APPROVED = 1
        private val log = LoggerFactory.getLogger(ModerationPhotoConsumer::class.java)
    }
}
