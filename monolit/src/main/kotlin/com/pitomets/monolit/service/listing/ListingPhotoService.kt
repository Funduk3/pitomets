package com.pitomets.monolit.service.listing

import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.kafka.moderation.producer.ModerationPhotoPublisher
import com.pitomets.monolit.model.entity.ListingPhoto
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationStatus
import com.pitomets.monolit.model.kafka.moderation.ModerationPhotoRequestedEvent
import com.pitomets.monolit.repository.AiPhotoReportRepo
import com.pitomets.monolit.repository.ListingPhotoRepo
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.service.MinioService
import com.pitomets.monolit.service.PhotoService
import com.pitomets.monolit.service.PhotoUrlService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class ListingPhotoService(
    private val listingPhotoRepo: ListingPhotoRepo,
    private val listingsRepo: ListingsRepo,
    private val minioService: MinioService,
    private val listingsService: ListingsService,
    private val moderationPhotoPublisher: ModerationPhotoPublisher,
    private val aiPhotoReportRepo: AiPhotoReportRepo,
    private val photoUrlService: PhotoUrlService,
) : PhotoService() {

    @Transactional
    fun uploadListingPhoto(
        file: MultipartFile,
        listingId: Long,
        userId: Long
    ): ListingPhoto {
        val listing = listingsService.requireOwnerAndReturnListing(listingId, userId)

        validateImage(file)

        val objectKey = buildPhotoKey(listingId, file)

        minioService.upload(
            objectName = objectKey,
            inputStream = file.inputStream,
            size = file.size,
            contentType = file.contentType ?: "image/jpeg"
        )

        val position = listingPhotoRepo
            .findByListingIdOrderByPosition(listingId)
            .size

        val photo = ListingPhoto(
            listingId = listingId,
            objectKey = objectKey,
            position = position
        )

        val saved = listingPhotoRepo.save(photo)

        moderationPhotoPublisher.publish(
            ModerationPhotoRequestedEvent(
                entityType = ModerationEntityType.LISTING,
                entityId = listingId,
                photoURI = photoUrlService.objectUrl(objectKey)
        ))

        if (position == 0) {
            listing.coverPhotoId = saved.id
            listingsRepo.save(listing)
        }
        return saved
    }

    @Transactional
    fun getListingPhotos(listingId: Long, includeUnapproved: Boolean): List<ListingPhoto> {
        listingsRepo.findById(listingId)
            .orElseThrow { ListingNotFoundException("Listing with id $listingId not found") }
        val photos = listingPhotoRepo.findByListingIdOrderByPosition(listingId)
        if (includeUnapproved) {
            return photos
        }

        val reportByUri = aiPhotoReportRepo.findByPhotoUriIn(
            photos.map { photoUrlService.objectUrl(it.objectKey) }
        )
            .associateBy { it.photoUri }

        return photos.filter { photo ->
            val photoUrl = photoUrlService.objectUrl(photo.objectKey)
            reportByUri[photoUrl]?.aiModerationStatus == ModerationStatus.APPROVED.name
        }
    }

    @Transactional
    fun deleteListingPhoto(
        listingId: Long,
        photoId: Long,
        userId: Long
    ) {
        val listing = listingsService.requireOwnerAndReturnListing(listingId, userId)

        val photo = listingPhotoRepo.findById(photoId)
            .orElseThrow { NoSuchElementException("Photo with id $photoId not found") }

        require(photo.listingId == listingId) {
            "Photo $photoId does not belong to listing $listingId"
        }

        listingPhotoRepo.delete(photo)
        minioService.delete(photo.objectKey)

        reindexPhotos(listingId)
        if (listing.coverPhotoId == photoId) {
            val first = listingPhotoRepo.findByListingIdOrderByPosition(listingId).firstOrNull()
            listing.coverPhotoId = first?.id
            listingsRepo.save(listing)
        }
    }

    // не используется ???
    fun deleteAllListingPhotos(listingId: Long) {
        val keys = listingPhotoRepo.findObjectKeysByListingId(listingId)

        deleteFromStorage(keys)
        deleteFromDatabase(listingId)
    }

    private fun deleteFromStorage(keys: List<String>) {
        keys.parallelStream().forEach { key -> minioService.delete(key) }
    }

    fun deleteFromDatabase(listingId: Long) {
        listingPhotoRepo.deleteAllByListingId(listingId)
    }

    private fun reindexPhotos(listingId: Long) {
        val photos = listingPhotoRepo.findByListingIdOrderByPosition(listingId)
        photos.forEachIndexed { index, photo ->
            photo.position = index
        }
        listingPhotoRepo.saveAll(photos)
    }

    private fun buildPhotoKey(listingId: Long, file: MultipartFile): String {
        val ext = extractExtension(file.originalFilename)
        return "listings/$listingId/${UUID.randomUUID()}.$ext"
    }
}
