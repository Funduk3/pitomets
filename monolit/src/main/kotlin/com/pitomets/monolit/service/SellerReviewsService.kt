package com.pitomets.monolit.service

import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.repository.ReviewsRepo
import org.springframework.stereotype.Service

@Service
class SellerReviewsService(
    private val reviewsRepo: ReviewsRepo,
) {
    fun getReviewsBySeller(sellerProfileId: Long): List<ReviewResponse> {
        return reviewsRepo.findBySellerProfileIdAndIsApprovedTrue(sellerProfileId).map { r ->
            ReviewResponse(
                id = requireNotNull(r.id),
                rating = r.rating,
                text = r.text,
                authorId = requireNotNull(r.author.id),
                listingId = requireNotNull(r.listing?.id),
                sellerProfileId = sellerProfileId,
                createdAt = r.createdAt,
            )
        }
    }
}
