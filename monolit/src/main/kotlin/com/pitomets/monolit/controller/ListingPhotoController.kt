package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.response.ListingPhotoResponse
import com.pitomets.monolit.model.dto.response.UploadPhotoResponse
import com.pitomets.monolit.service.listing.ListingPhotoService
import com.pitomets.monolit.service.listing.ListingsService
import com.pitomets.monolit.service.PhotoUrlService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/listings/{listingId}/photos")
class ListingPhotoController(
    private val listingsService: ListingsService,
    private val listingPhotoService: ListingPhotoService,
    private val photoUrlService: PhotoUrlService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhoto(
        @PathVariable listingId: Long,
        @RequestParam file: MultipartFile,
        @AuthenticationPrincipal user: UserPrincipal
    ): UploadPhotoResponse {
        val photo = listingPhotoService.uploadListingPhoto(
            file = file,
            listingId = listingId,
            userId = user.id
        )

        return UploadPhotoResponse(
            photoId = photo.id,
            objectKey = photo.objectKey,
            position = photo.position
        )
    }

    @GetMapping
    fun getListingPhotos(
        @PathVariable listingId: Long,
        @AuthenticationPrincipal user: UserPrincipal?
    ): ListingPhotoResponse {
        val isAdmin = user?.authorities?.any { it.authority == "ROLE_ADMIN" } == true
        val listing = listingsService.getListingEntity(listingId)
        if (!isAdmin) {
            listingsService.getListing(listingId, user?.id)
        }
        val isOwner = user?.id != null && listing.sellerProfile.seller?.id == user.id
        val title = listing.title
        val photos = listingPhotoService.getListingPhotos(
            listingId,
            includeUnapproved = isAdmin || isOwner
        )

        return ListingPhotoResponse(
            title = title,
            photos = photos.map { photoUrlService.objectUrl(it.objectKey) },
            photoIds = photos.map { it.id }
        )
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePhoto(
        @PathVariable listingId: Long,
        @PathVariable photoId: Long,
        @AuthenticationPrincipal user: UserPrincipal
    ) {
        listingPhotoService.deleteListingPhoto(
            listingId = listingId,
            photoId = photoId,
            userId = user.id
        )
    }
}
