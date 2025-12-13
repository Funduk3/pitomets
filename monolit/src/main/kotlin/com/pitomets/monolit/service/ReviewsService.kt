package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.ReviewsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

/*
Сейчас будто бы слишком много лишних походов в БД (даже lazy) в таком сервисе
Подумать как починить + написать тесты + переделать avg оценку + нельзя писать отзыв на
самого себя
 */
@Service
class ReviewsService(
    private val reviewsRepo: ReviewsRepo,
    private val userRepo: UserRepo,
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo,
) {
    fun createReview(
        authorId: Long,
        request: CreateReviewRequest
    ): ReviewResponse {
        val author = userRepo
            .findById(authorId)
            .orElseThrow { UserNotFoundException("User not found") }

        val listing = request
            .listingId.let {
                listingsRepo.findById(it)
                    .orElseThrow { ListingNotFoundException("Listing not found") }
            }

        val sellerProfile = listing.sellerProfile

        val review = Review(
            rating = request.rating,
            text = request.text,
            createdAt = OffsetDateTime.now(),
            author = author,
            sellerProfile = sellerProfile,
            listing = listing
        )

        val saved = reviewsRepo.save(review)

        // todo fix this shit
        val reviewsForSeller = reviewsRepo.findBySellerProfileId(sellerProfile.id!!)
        sellerProfile.rating = if (reviewsForSeller.isEmpty()) 0.0 else reviewsForSeller.map { it.rating }.average()

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
