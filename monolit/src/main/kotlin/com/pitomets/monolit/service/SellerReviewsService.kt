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
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class SellerReviewsService(
    private val reviewsRepo: ReviewsRepo,
    private val userRepo: UserRepo,
    private val sellerProfileRepo: SellerProfileRepo,
) {
    fun getSellerProfileOrThrow(sellerId: Long): SellerProfile =
        sellerProfileRepo.findBySeller_Id(sellerId)
            ?: throw EntityNotFoundException("SellerProfile not found for sellerId=$sellerId")

    fun getReviewsBySeller(sellerProfileId: Long): List<ReviewResponse> {
        // Получаем профиль продавца по ID пользователя
        val sellerProfile = getSellerProfileOrThrow(sellerProfileId)

        // Ищем отзывы по реальному ID профиля
        return reviewsRepo.findBySellerProfileId(sellerProfile.id!!).map { r ->
            ReviewResponse(
                id = requireNotNull(r.id),
                rating = r.rating,
                text = r.text,
                authorId = requireNotNull(r.author.id),
                listingId = r.listing?.id,
                sellerProfileId = requireNotNull(sellerProfile.id),
                createdAt = r.createdAt,
            )
        }
    }

    fun createSellerReview(sellerProfileId: Long, authorId: Long, request: CreateReviewRequest): ReviewResponse {
        val author = userRepo.findUserOrThrow(authorId)
        val sellerProfile = getSellerProfileOrThrow(sellerProfileId)
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
        println("CREATED THIS REVIEW")
        println(review.id)
        println(review.sellerProfile.id)
        println("------------------")
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

    fun deleteSellerReview(sellerProfileId: Long, userId: Long, reviewId: Long) {
        // Получаем профиль продавца по ID пользователя (seller.id = user_id)
        val sellerProfile = getSellerProfileOrThrow(sellerProfileId)
        println("Found seller profile: id=${sellerProfile.id}, seller.id=${sellerProfile.seller?.id}")

        // Ищем отзыв по reviewId И реальному ID профиля (не по userId!)
        val review: Review = requireNotNull(
            reviewsRepo.findByIdAndSellerProfile_Id(reviewId, sellerProfile.id!!)
        ) {
            "Review not found for reviewId=$reviewId and sellerProfile.id=${sellerProfile.id}"
        }
        println("Found review: id=${review.id}")

        if (userId != review.author.id) {
            throw BadReviewException("Only author can delete a review")
        }
        println("Authorization passed")

        sellerProfile.sumReviews -= review.rating
        sellerProfile.countReviews -= 1
        if (sellerProfile.countReviews > 0) {
            sellerProfile.rating = sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()
        } else {
            sellerProfile.rating = 0.0
        }

        sellerProfileRepo.save(sellerProfile)
        reviewsRepo.deleteById(reviewId)
    }

    @Transactional
    fun updateSellerReview(
        sellerProfileId: Long,
        currentUserId: Long,
        request: UpdateSellerReviewRequest
    ): ReviewResponse {
        // Получаем профиль продавца по ID пользователя
        val sellerProfile = getSellerProfileOrThrow(sellerProfileId)

        // Ищем отзыв по реальному ID профиля продавца
        val review = reviewsRepo.findBySellerProfileIdAndAuthorId(sellerProfile.id!!, currentUserId)
            ?: throw BadReviewException("Review not found or access denied")

        sellerProfile.sumReviews -= review.rating
        sellerProfile.sumReviews += request.rating
        sellerProfile.rating = sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()

        review.text = request.text
        review.rating = request.rating
        review.createdAt = OffsetDateTime.now()

        val saved = reviewsRepo.save(review)

        return ReviewResponse(
            id = requireNotNull(saved.id),
            rating = saved.rating,
            text = saved.text,
            authorId = currentUserId,
            listingId = null,
            sellerProfileId = requireNotNull(sellerProfile.id),
            createdAt = saved.createdAt
        )
    }
}
