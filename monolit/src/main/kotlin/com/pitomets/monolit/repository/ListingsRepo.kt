package com.pitomets.monolit.repository

import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.SellerProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
interface ListingsRepo : JpaRepository<Listing, Long> {
    fun findAllByIdIn(ids: List<Long>): List<Listing>
    fun findBySellerProfile(sellerProfile: SellerProfile): List<Listing>
    fun findByIsArchivedFalseOrderByIdDesc(pageable: Pageable): List<Listing>
    fun findByIsArchivedFalseAndIdLessThanOrderByIdDesc(id: Long, pageable: Pageable): List<Listing>

}

fun ListingsRepo.findListingOrThrow(listingId: Long): Listing =
    findByIdOrNull(listingId)
        ?: throw ListingNotFoundException("Listing with id $listingId not found")
