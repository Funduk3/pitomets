package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.response.ListingPhotoResponse
import com.pitomets.monolit.model.dto.response.UploadPhotoResponse
import com.pitomets.monolit.service.ListingPhotoService
import com.pitomets.monolit.service.ListingsService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
    private val listingPhotoService: ListingPhotoService
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
        @PathVariable listingId: Long
    ): ListingPhotoResponse {
        val listing = listingsService.getListing(listingId)
        val photos = listingPhotoService.getListingPhotos(listingId)

        return ListingPhotoResponse(
            title = listing.title ?: "Untitled",
            photos = photos.map { "/listings/$listingId/photos/${it.id}" }
        )
    }

    @GetMapping("/{photoId}")
    fun getPhoto(
        @PathVariable photoId: Long,
        @PathVariable listingId: Long
    ): ResponseEntity<InputStreamResource> {
        val inputStream = listingPhotoService.downloadListingPhoto(
            listingId = listingId,
            photoId = photoId
        )

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(InputStreamResource(inputStream))
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
