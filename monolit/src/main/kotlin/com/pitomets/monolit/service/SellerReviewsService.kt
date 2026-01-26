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
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class SellerReviewsService(
    private val reviewsRepo: ReviewsRepo,
    private val userRepo: UserRepo,
    private val sellerProfileRepo: SellerProfileRepo,
) {
    fun getReviewsBySeller(sellerProfileId: Long): List<ReviewResponse> =
        reviewsRepo.findBySellerProfileId(sellerProfileId).map {
                r ->
            ReviewResponse(
                id = requireNotNull(r.id),
                rating = r.rating,
                text = r.text,
                authorId = requireNotNull(r.author.id),
                listingId = requireNotNull(r.listing?.id),
                sellerProfileId = sellerProfileId,
                createdAt = OffsetDateTime.now(),
            )
        }

    fun createSellerReview(sellerProfileId: Long, authorId: Long, request: CreateReviewRequest): ReviewResponse {
        val author = userRepo.findUserOrThrow(authorId)
        val sellerProfile = sellerProfileRepo.findBySellerIdOrThrow(sellerProfileId)
        if (sellerProfileId == authorId) {
            throw BadReviewException("Seller cannot create review on their own")
        }
        if (reviewsRepo.findBySellerProfileIdAndAuthorId(request.listingId, authorId) != null) {
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

        sellerProfileRepo.save(sellerProfile)

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

    // такой же код в другом сервисе, не думаю, что нужно как-то удалять код
    fun deleteSellerReview(sellerProfileId: Long, userId: Long, reviewId: Long) {
        val review: Review = requireNotNull(reviewsRepo.findBySellerProfileIdAndReviewId(sellerProfileId, reviewId))
        if (userId != review.author.id) {
            throw BadReviewException("Only author can delete a review")
        }

        val sellerProfile: SellerProfile = requireNotNull(sellerProfileRepo.findBySellerId(sellerProfileId))
        sellerProfile.sumReviews -= review.rating
        sellerProfile.countReviews -= 1
        sellerProfile.rating =
            sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()

        reviewsRepo.deleteById(reviewId)
    }

    fun updateSellerReview(sellerProfileId: Long, authorId: Long, request: UpdateSellerReviewRequest): ReviewResponse {
        if (authorId != request.authorId) {
            throw BadReviewException("User cannot change other's review")
        }
        val sellerProfile = sellerProfileRepo.findBySellerIdOrThrow(sellerProfileId)
        val review = reviewsRepo.findBySellerProfileIdAndAuthorId(sellerProfileId, authorId)
            ?: throw IllegalArgumentException("Review not found")

        // думаю работу над рейтингом надо в отдельный сервис или метод
        sellerProfile.sumReviews -= review.rating
        sellerProfile.sumReviews += request.rating
        sellerProfile.rating =
            sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()

        review.text = request.text
        review.createdAt = OffsetDateTime.now()
        review.rating = request.rating

        val saved = reviewsRepo.save(review)
        return ReviewResponse(
            id = requireNotNull(saved?.id),
            rating = request.rating,
            text = request.text,
            authorId = authorId,
            listingId = null,
            sellerProfileId = sellerProfileId,
            createdAt = saved.createdAt
        )
    }
}
