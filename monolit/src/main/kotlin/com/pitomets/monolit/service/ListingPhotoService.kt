package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.model.entity.ListingPhoto
import com.pitomets.monolit.repository.ListingPhotoRepo
import com.pitomets.monolit.repository.ListingsRepo
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.*

@Service
class ListingPhotoService(
    private val listingPhotoRepo: ListingPhotoRepo,
    private val listingsRepo: ListingsRepo,
    private val minioService: MinioService,
    private val listingsService: ListingsService,
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
        if (position == 0) {
            listing.coverPhotoId = saved.id
            listingsRepo.save(listing)
        }
        return saved
    }

    @Transactional
    fun downloadListingPhoto(
        listingId: Long,
        photoId: Long
    ): InputStream {
        val photo = listingPhotoRepo.findById(photoId)
            .orElseThrow { NoSuchElementException("Photo with id $photoId not found") }

        require(photo.listingId == listingId) {
            "Photo $photoId does not belong to listing $listingId"
        }

        return minioService.download(photo.objectKey)
    }

    @Transactional
    fun getListingPhotos(listingId: Long): List<ListingPhoto> {
        listingsRepo.findById(listingId)
            .orElseThrow { ListingNotFoundException("Listing with id $listingId not found") }
        return listingPhotoRepo.findByListingIdOrderByPosition(listingId)
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
