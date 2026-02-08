package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.AvatarNotFoundException
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.repository.findUserOrThrow
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.UUID

@Service
class UserPhotoService(
    private val minioService: MinioService,
    private val userRepo: UserRepo,
    private val listingsRepo: ListingsRepo
) : PhotoService() {

    @Transactional
    fun uploadAvatar(file: MultipartFile, userId: Long): String {
        validateImage(file)

        val user = userRepo.findUserOrThrow(userId)

        // Удаляем старый аватар, если есть
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
        listingsRepo.updateSellerAvatarKeyBySellerId(userId, objectKey)

        return objectKey
    }

    @Transactional
    fun downloadAvatar(userId: Long): InputStream {
        val user = userRepo.findUserOrThrow(userId)

        val avatarKey = user.avatarKey
            ?: throw AvatarNotFoundException("User $userId has no avatar")

        return minioService.download(avatarKey)
    }

    @Transactional
    fun deleteAvatar(userId: Long): String {
        val user = userRepo.findUserOrThrow(userId)

        val avatarKey = user.avatarKey
            ?: throw AvatarNotFoundException("User $userId has no avatar")

        minioService.delete(avatarKey)

        user.avatarKey = null
        userRepo.save(user)
        listingsRepo.updateSellerAvatarKeyBySellerId(userId, null)

        return avatarKey
    }

    private fun buildAvatarKey(userId: Long, file: MultipartFile): String {
        val ext = extractExtension(file.originalFilename)
        return "avatars/$userId/${UUID.randomUUID()}.$ext"
    }
}
