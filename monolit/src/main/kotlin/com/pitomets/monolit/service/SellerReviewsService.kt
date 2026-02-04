package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.BadReviewException
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.request.UpdateSellerReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.repository.ReviewsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.repository.findUserOrThrow
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class SellerReviewsService(
    private val reviewsRepo: ReviewsRepo,
    private val userRepo: UserRepo,
    private val sellerProfileRepo: SellerProfileRepo,
) {
    fun getSellerProfileOrThrowById(userId: Long): SellerProfile {
        return sellerProfileRepo.findById(userId).orElseThrow {
            BadReviewException("Seller profile not found for userId: $userId")
        }
    }

    fun getReviewsBySeller(sellerProfileId: Long): List<ReviewResponse> {
        return reviewsRepo.findBySellerProfileId(sellerProfileId).map { r ->
            ReviewResponse(
                id = requireNotNull(r.id),
                rating = r.rating,
                text = r.text,
                authorId = requireNotNull(r.author.id),
                listingId = r.listing?.id,
                sellerProfileId = sellerProfileId,
                createdAt = r.createdAt,
            )
        }
    }

    @Transactional
    fun createSellerReview(sellerProfileId: Long, authorId: Long, request: CreateReviewRequest): ReviewResponse {
        val author = userRepo.findUserOrThrow(authorId)
        val sellerProfile = getSellerProfileOrThrowById(sellerProfileId)
        if (sellerProfileId == authorId) {
            throw BadReviewException("Seller cannot create review on their own")
        }
        if (reviewsRepo.findBySellerProfileIdAndAuthorId(sellerProfileId, authorId) != null) {
            throw BadReviewException("Review by this author already exists")
        }

        val review = Review(
            rating = request.rating,
            text = request.text,
            createdAt = OffsetDateTime.now(),
            author = author,
            sellerProfile = sellerProfile,
            listing = null
        )
        val saved = reviewsRepo.save(review)

        sellerProfile.sumReviews += request.rating
        sellerProfile.countReviews += 1
        sellerProfile.rating =
            sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()

        return ReviewResponse(
            id = requireNotNull(saved.id),
            rating = request.rating,
            text = request.text,
            authorId = requireNotNull(author.id),
            listingId = null,
            sellerProfileId = requireNotNull(sellerProfile.id),
            createdAt = saved.createdAt,
        )
    }

    @Transactional
    fun deleteSellerReview(sellerProfileId: Long, userId: Long, reviewId: Long) {
        val sellerProfile = getSellerProfileOrThrowById(sellerProfileId)

        val review: Review = requireNotNull(
            reviewsRepo.findByIdAndSellerProfile_Id(reviewId, sellerProfile.id!!)
        ) {
            "Review not found for reviewId=$reviewId and sellerProfile.id=${sellerProfile.id}"
        }

        if (userId != review.author.id) {
            throw BadReviewException("Only author can delete a review")
        }

        sellerProfile.sumReviews -= review.rating
        sellerProfile.countReviews -= 1
        if (sellerProfile.countReviews > 0) {
            sellerProfile.rating = sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()
        } else {
            sellerProfile.rating = 0.0
        }
        reviewsRepo.deleteById(reviewId)
    }

    @Transactional
    fun updateSellerReview(
        sellerProfileId: Long,
        currentUserId: Long,
        request: UpdateSellerReviewRequest
    ): ReviewResponse {
        val sellerProfile = getSellerProfileOrThrowById(sellerProfileId)

        val review = reviewsRepo.findBySellerProfileIdAndAuthorId(sellerProfile.id!!, currentUserId)
            ?: throw BadReviewException("Review not found or access denied")

        sellerProfile.sumReviews -= review.rating
        sellerProfile.sumReviews += request.rating
        sellerProfile.rating = sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()

        review.text = request.text
        review.rating = request.rating
        review.createdAt = OffsetDateTime.now()

        return ReviewResponse(
            id = requireNotNull(review.id),
            rating = review.rating,
            text = review.text,
            authorId = currentUserId,
            listingId = null,
            sellerProfileId = requireNotNull(sellerProfile.id),
            createdAt = review.createdAt
        )
    }
}
