package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.Review
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReviewsRepo : JpaRepository<Review, Long> {
    fun findBySellerProfileId(sellerProfileId: Long): List<Review>
    fun findByListingId(listingId: Long): List<Review>
    fun findByListingIdAndAuthorId(listingId: Long, authorId: Long): Review?

    @Suppress("FunctionNaming")
    fun findByIdAndSellerProfile_Id(id: Long, sellerProfileId: Long): Review?

    fun findBySellerProfileIdAndAuthorId(sellerId: Long, authorId: Long): Review?
}
