package com.pitomets.monolit.service.listing

import com.pitomets.monolit.exceptions.BadReviewException
import com.pitomets.monolit.model.dto.request.AdminMessage
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.request.UpdateListingReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.model.kafka.moderation.ModerationOperation
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.ReviewsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.repository.findListingOrThrow
import com.pitomets.monolit.repository.findUserOrThrow
import com.pitomets.monolit.service.moderation.ModerationRequestService
import com.pitomets.monolit.service.moderationHint
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class ListingReviewsService(
    private val reviewsRepo: ReviewsRepo,
    private val userRepo: UserRepo,
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo,
    private val moderationRequestService: ModerationRequestService,
) {
    private val log = LoggerFactory.getLogger(ListingReviewsService::class.java)

    @Transactional
    fun createListingReview(
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

        // нельзя более 1 отзыва от 1 человека, а то будут крутить
        if (reviewsRepo.findByListingIdAndAuthorId(request.listingId, authorId) != null) {
            throw BadReviewException("Review by this author already exists")
        }

        val review = Review(
            rating = request.rating,
            text = request.text,
            createdAt = OffsetDateTime.now(),
            author = author,
            sellerProfile = sellerProfile,
            listing = listing
        )

        val saved = reviewsRepo.save(review)
        moderationRequestService.publishReview(saved, ModerationOperation.CREATE)

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
            listingId = requireNotNull(listing.id),
            sellerProfileId = requireNotNull(sellerProfile.id),
            createdAt = saved.createdAt,
            moderationHint = moderationHint(
                status = saved.aiModerationStatus,
                reason = saved.aiModerationReason,
                toxicityScore = saved.aiToxicityScore,
                profanityDetected = saved.aiProfanityDetected,
                sexualContentDetected = saved.aiSexualContentDetected,
                sourceAction = saved.aiSourceAction,
                modelVersion = saved.aiModelVersion
            )
        )
    }

    fun getReviewByListing(listingId: Long): List<ReviewResponse> =
        reviewsRepo.findByListingIdAndIsApprovedTrue(listingId).map { r ->
            ReviewResponse(
                id = requireNotNull(r.id),
                rating = r.rating,
                text = r.text,
                authorId = requireNotNull(r.author.id),
                listingId = requireNotNull(r.listing?.id),
                sellerProfileId = requireNotNull(r.sellerProfile.id),
                createdAt = r.createdAt,
                moderationHint = moderationHint(
                    status = r.aiModerationStatus,
                    reason = r.aiModerationReason,
                    toxicityScore = r.aiToxicityScore,
                    profanityDetected = r.aiProfanityDetected,
                    sexualContentDetected = r.aiSexualContentDetected,
                    sourceAction = r.aiSourceAction,
                    modelVersion = r.aiModelVersion
                )
            )
        }

    fun getPendingReviews(): List<ReviewResponse> {
        return reviewsRepo.findByIsApprovedFalse().map { r ->
            ReviewResponse(
                id = requireNotNull(r.id),
                rating = r.rating,
                text = r.text,
                authorId = requireNotNull(r.author.id),
                listingId = requireNotNull(r.listing?.id),
                sellerProfileId = requireNotNull(r.sellerProfile.id),
                createdAt = r.createdAt,
                moderationHint = moderationHint(
                    status = r.aiModerationStatus,
                    reason = r.aiModerationReason,
                    toxicityScore = r.aiToxicityScore,
                    profanityDetected = r.aiProfanityDetected,
                    sexualContentDetected = r.aiSexualContentDetected,
                    sourceAction = r.aiSourceAction,
                    modelVersion = r.aiModelVersion
                )
            )
        }
    }

    fun getPendingReview(id: Long): ReviewResponse {
        val review = reviewsRepo.findByIdAndIsApprovedFalse(id)
            ?: throw BadReviewException("Pending review with id $id not found")
        return ReviewResponse(
            id = requireNotNull(review.id),
            rating = review.rating,
            text = review.text,
            authorId = requireNotNull(review.author.id),
            listingId = requireNotNull(review.listing?.id),
            sellerProfileId = requireNotNull(review.sellerProfile.id),
            createdAt = review.createdAt,
            moderationHint = moderationHint(
                status = review.aiModerationStatus,
                reason = review.aiModerationReason,
                toxicityScore = review.aiToxicityScore,
                profanityDetected = review.aiProfanityDetected,
                sexualContentDetected = review.aiSexualContentDetected,
                sourceAction = review.aiSourceAction,
                modelVersion = review.aiModelVersion
            )
        )
    }

    @Transactional
    fun approveReview(id: Long) {
        val review = reviewsRepo.findById(id)
            .orElseThrow { BadReviewException("Review not found") }
        review.isApproved = true
        reviewsRepo.save(review)
    }

    @Transactional
    fun declineReview(id: Long, adminMessage: AdminMessage) {
        val review = reviewsRepo.findByIdAndIsApprovedFalse(id)
            ?: throw BadReviewException("Pending review with id $id not found")
        log.info("Declining review id={} with reason={}", id, adminMessage.message)

        val sellerProfile = review.sellerProfile
        sellerProfile.sumReviews -= review.rating
        sellerProfile.countReviews -= 1
        sellerProfile.rating =
            if (sellerProfile.countReviews.toInt() == 0) {
                0.0
            } else {
                sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()
            }
        sellerProfileRepo.save(sellerProfile)

        reviewsRepo.delete(review)
    }

    @Transactional
    fun deleteListingReview(
        currentUserId: Long,
        reviewId: Long
    ) {
        val review = reviewsRepo.findById(reviewId)
            .orElseThrow { BadReviewException("Review not found") }

        if (review.author.id != currentUserId) {
            throw BadReviewException("Only author can delete a review")
        }

        val sellerProfile = review.sellerProfile

        sellerProfile.sumReviews -= review.rating
        sellerProfile.countReviews -= 1

        sellerProfile.rating =
            if (sellerProfile.countReviews.toInt() == 0) {
                0.0
            } else {
                sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()
            }

        reviewsRepo.delete(review)
    }

    @Transactional
    fun updateListingReview(
        currentUserId: Long,
        request: UpdateListingReviewRequest
    ): ReviewResponse {
        val review = reviewsRepo.findByListingIdAndAuthorId(
            request.listingId,
            currentUserId
        ) ?: throw BadReviewException("Review not found or access denied")

        val sellerProfile = review.sellerProfile

        sellerProfile.sumReviews -= review.rating
        sellerProfile.sumReviews += request.rating
        sellerProfile.rating =
            sellerProfile.sumReviews.toDouble() / sellerProfile.countReviews.toDouble()

        review.text = request.text
        review.rating = request.rating
        review.createdAt = OffsetDateTime.now()
        val updated = reviewsRepo.save(review)
        moderationRequestService.publishReview(updated, ModerationOperation.UPDATE)

        return ReviewResponse(
            id = updated.id!!,
            rating = updated.rating,
            text = updated.text,
            authorId = currentUserId,
            listingId = updated.listing!!.id!!,
            sellerProfileId = updated.sellerProfile.id!!,
            createdAt = updated.createdAt,
            moderationHint = moderationHint(
                status = updated.aiModerationStatus,
                reason = updated.aiModerationReason,
                toxicityScore = updated.aiToxicityScore,
                profanityDetected = updated.aiProfanityDetected,
                sexualContentDetected = updated.aiSexualContentDetected,
                sourceAction = updated.aiSourceAction,
                modelVersion = updated.aiModelVersion
            )
        )
    }
}
