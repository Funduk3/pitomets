package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.BadReviewException
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.ReviewsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.utils.findListingOrThrow
import com.pitomets.monolit.utils.findUserOrThrow
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class ReviewsService(
    private val reviewsRepo: ReviewsRepo,
    private val userRepo: UserRepo,
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo,
) {
    @Transactional
    fun createReview(
        authorId: Long,
        request: CreateReviewRequest
    ): ReviewResponse {
        val author = userRepo.findUserOrThrow(authorId)

        val listing = listingsRepo.findListingOrThrow(request.listingId)

        val sellerProfile = listing.sellerProfile
        val sellerUserId = sellerProfile.seller?.id

        if (sellerUserId == null) {
            throw BadReviewException("Seller profile has no associated user")
        }

        if (authorId == sellerUserId) {
            throw BadReviewException("User cannot write a review on their own listing")
        }

        val review = Review(
            rating = request.rating,
            starsNumber = request.rating,
            text = request.text,
            createdAt = OffsetDateTime.now(),
            author = author,
            sellerProfile = sellerProfile,
            listing = listing
        )

        val saved = reviewsRepo.save(review)

        sellerProfile.sumReviews += request.rating
        sellerProfile.countReviews += 1
        sellerProfile.rating =
            sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()

        sellerProfileRepo.save(sellerProfile)

        return ReviewResponse(
            id = requireNotNull(saved.id),
            rating = request.rating,
            text = request.text,
            authorId = requireNotNull(author.id),
            listingId = listing.id,
            sellerProfileId = requireNotNull(sellerProfile.id),
            createdAt = saved.createdAt,
        )
    }

    fun getByListing(listingId: Long): List<ReviewResponse> =
        reviewsRepo.findByListingId(listingId).map { r ->
            ReviewResponse(
                id = requireNotNull(r.id),
                rating = r.rating,
                text = r.text,
                authorId = requireNotNull(r.author.id),
                listingId = r.listing?.id,
                sellerProfileId = requireNotNull(r.sellerProfile.id),
                createdAt = r.createdAt
            )
        }

    fun getBySeller(sellerProfileId: Long): List<ReviewResponse> =
        reviewsRepo.findBySellerProfileId(sellerProfileId).map { r ->
            ReviewResponse(
                id = requireNotNull(r.id),
                rating = r.rating,
                text = r.text,
                authorId = requireNotNull(r.author.id),
                listingId = r.listing?.id,
                sellerProfileId = requireNotNull(r.sellerProfile.id),
                createdAt = r.createdAt
            )
        }
}
