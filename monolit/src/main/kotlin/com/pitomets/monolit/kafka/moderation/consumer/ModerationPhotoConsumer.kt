package com.pitomets.monolit.kafka.moderation.consumer

import com.pitomets.monolit.model.entity.AiPhotoModerationReport
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationPhotoProcessedEvent
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.repository.AiPhotoReportRepo
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.UserRepo
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ModerationPhotoConsumer(
    private val listingsRepo: ListingsRepo,
    private val userRepo: UserRepo,
    private val aiPhotoModerationReport: AiPhotoReportRepo,
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
        val report = aiPhotoModerationReport.findByPhotoUri(event.photoURI)
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


    companion object {
        private val log = LoggerFactory.getLogger(ModerationPhotoConsumer::class.java)
    }
}
