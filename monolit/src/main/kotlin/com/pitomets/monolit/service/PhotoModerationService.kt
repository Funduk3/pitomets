package com.pitomets.monolit.service

import com.pitomets.monolit.model.dto.response.ModerationPhotoItemResponse
import com.pitomets.monolit.model.dto.response.PhotoModerationHintResponse
import com.pitomets.monolit.model.entity.AiPhotoModerationReport
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.repository.AiPhotoReportRepo
import com.pitomets.monolit.repository.ListingPhotoRepo
import com.pitomets.monolit.repository.UserRepo
import org.springframework.stereotype.Service

@Service
class PhotoModerationService(
    private val listingPhotoRepo: ListingPhotoRepo,
    private val userRepo: UserRepo,
    private val aiPhotoReportRepo: AiPhotoReportRepo,
    private val photoUrlService: PhotoUrlService,
) {
    fun getPendingPhotos(): List<ModerationPhotoItemResponse> {
        val listingPhotos = listingPhotoRepo.findAll()
        val avatarUsers = userRepo.findByAvatarKeyIsNotNull()

        val allPhotoUris = mutableSetOf<String>()
        listingPhotos.mapTo(allPhotoUris) { photoUrlService.objectUrl(it.objectKey) }
        avatarUsers.mapTo(allPhotoUris) { photoUrlService.objectUrl(it.avatarKey!!) }

        if (allPhotoUris.isEmpty()) {
            return emptyList()
        }

        val reportsByUri = aiPhotoReportRepo.findByPhotoUriIn(allPhotoUris.toList())
            .associateBy { it.photoUri }

        val listingItems = listingPhotos.mapNotNull { photo ->
            val photoUrl = photoUrlService.objectUrl(photo.objectKey)
            val report = reportsByUri[photoUrl]
            if (!shouldShow(report)) {
                return@mapNotNull null
            }
            ModerationPhotoItemResponse(
                entityType = ModerationEntityType.LISTING.name,
                entityId = photo.listingId,
                photoId = photo.id,
                photoUrl = photoUrl,
                photoUri = photoUrl,
                moderationHint = toHint(report)
            )
        }

        val avatarItems = avatarUsers.mapNotNull { user ->
            val avatarKey = user.avatarKey ?: return@mapNotNull null
            val avatarUrl = photoUrlService.objectUrl(avatarKey)
            val report = reportsByUri[avatarUrl]
            if (!shouldShow(report)) {
                return@mapNotNull null
            }
            ModerationPhotoItemResponse(
                entityType = ModerationEntityType.USER.name,
                entityId = requireNotNull(user.id),
                photoId = null,
                photoUrl = avatarUrl,
                photoUri = avatarUrl,
                moderationHint = toHint(report)
            )
        }

        return listingItems + avatarItems
    }

    private fun shouldShow(report: AiPhotoModerationReport?): Boolean {
        val status = report?.aiModerationStatus
        return status == null || status != ModerationStatus.APPROVED.name
    }

    private fun toHint(report: AiPhotoModerationReport?): PhotoModerationHintResponse? {
        if (report == null) {
            return null
        }
        return PhotoModerationHintResponse(
            status = report.aiModerationStatus,
            reason = report.aiModerationReason,
            toxicityScore = report.aiToxicityScore,
            labels = report.aiLabels,
            toxicTextDetected = report.aiToxicTextDetected,
            toxicTextMatches = report.aiToxicTextMatches
        )
    }
}
