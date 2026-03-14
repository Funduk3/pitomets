package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.AvatarNotFoundException
import com.pitomets.monolit.kafka.moderation.producer.ModerationPhotoPublisher
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.model.kafka.moderation.ModerationPhotoRequestedEvent
import com.pitomets.monolit.repository.AiPhotoReportRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.repository.findUserOrThrow
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class UserPhotoService(
    private val minioService: MinioService,
    private val userRepo: UserRepo,
    private val photoUrlService: PhotoUrlService,
    private val moderationPhotoPublisher: ModerationPhotoPublisher,
    private val aiPhotoReportRepo: AiPhotoReportRepo,
) : PhotoService() {

    @Transactional
    fun uploadAvatar(file: MultipartFile, userId: Long): String {
        validateImage(file)

        val user = userRepo.findUserOrThrow(userId)

        user.avatarKey?.let { oldKey ->
            minioService.delete(oldKey)
        }

        val objectKey = buildAvatarKey(userId, file)

        minioService.upload(
            objectName = objectKey,
            inputStream = file.inputStream,
            size = file.size,
            contentType = file.contentType ?: "image/jpeg"
        )

        user.avatarKey = objectKey
        userRepo.save(user)

        moderationPhotoPublisher.publish(
            ModerationPhotoRequestedEvent(
                entityType = ModerationEntityType.USER,
                entityId = userId,
                photoURI = photoUrlService.objectUrl(objectKey)
            )
        )

        return objectKey
    }

    @Transactional
    fun getAvatarKey(userId: Long): String {
        val user = userRepo.findUserOrThrow(userId)

        return user.avatarKey
            ?: throw AvatarNotFoundException("User $userId has no avatar")
    }

    @Transactional
    fun getAvatarUrlOrNull(userId: Long, includeUnapproved: Boolean): String? {
        val user = userRepo.findUserOrThrow(userId)
        val avatarKey = user.avatarKey
        if (avatarKey == null) {
            return null
        }

        val avatarUrl = photoUrlService.objectUrl(avatarKey)
        val approved = if (includeUnapproved) {
            true
        } else {
            val report = aiPhotoReportRepo.findByPhotoUri(avatarUrl)
            report?.aiModerationStatus == ModerationStatus.APPROVED.name
        }
        return avatarUrl.takeIf { approved }
    }

    @Transactional
    fun deleteAvatar(userId: Long): String {
        val user = userRepo.findUserOrThrow(userId)

        val avatarKey = user.avatarKey
            ?: throw AvatarNotFoundException("User $userId has no avatar")

        minioService.delete(avatarKey)

        user.avatarKey = null
        userRepo.save(user)

        return avatarKey
    }

    private fun buildAvatarKey(userId: Long, file: MultipartFile): String {
        val ext = extractExtension(file.originalFilename)
        return "avatars/$userId/${UUID.randomUUID()}.$ext"
    }
}
