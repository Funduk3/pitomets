package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.ListingPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ListingPhotoRepo : JpaRepository<ListingPhoto, Long> {

    fun findByListingIdOrderByPosition(listingId: Long): List<ListingPhoto>

    fun deleteByListingIdAndId(listingId: Long, photoId: Long)

    fun deleteAllByListingId(listingId: Long)

    fun findObjectKeysByListingId(listingId: Long): List<String>

    fun findByObjectKey(objectKey: String): ListingPhoto
}
