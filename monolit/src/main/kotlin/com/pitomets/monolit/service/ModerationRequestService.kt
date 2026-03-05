package com.pitomets.monolit.service

import com.pitomets.monolit.kafka.moderation.producer.ModerationPublisher
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.Review
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.kafka.moderation.ModerationOperation
import com.pitomets.monolit.model.kafka.moderation.ModerationRequestedEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ModerationRequestService(
    private val moderationPublisher: ModerationPublisher,
    @Value("\${moderation.with-animal:true}")
    private val withAnimal: Boolean
) {
    fun publishListing(
        listing: Listing,
        operation: ModerationOperation
    ) {
        val listingId = requireNotNull(listing.id) { "Listing ID cannot be null for moderation request" }
        moderationPublisher.publish(
            ModerationRequestedEvent(
                entityType = ModerationEntityType.LISTING,
                entityId = listingId,
                operation = operation,
                textParts = listOf(listing.title, listing.description),
                withAnimal = withAnimal
            )
        )
    }

    fun publishSellerProfile(
        profile: SellerProfile,
        operation: ModerationOperation
    ) {
        val profileId = requireNotNull(profile.id) { "Seller profile ID cannot be null for moderation request" }
        moderationPublisher.publish(
            ModerationRequestedEvent(
                entityType = ModerationEntityType.SELLER_PROFILE,
                entityId = profileId,
                operation = operation,
                textParts = listOfNotNull(profile.shopName, profile.description),
                withAnimal = withAnimal
            )
        )
    }

    fun publishReview(
        review: Review,
        operation: ModerationOperation
    ) {
        val reviewId = requireNotNull(review.id) { "Review ID cannot be null for moderation request" }
        moderationPublisher.publish(
            ModerationRequestedEvent(
                entityType = ModerationEntityType.REVIEW,
                entityId = reviewId,
                operation = operation,
                textParts = listOfNotNull(review.text),
                withAnimal = withAnimal
            )
        )
    }
}
