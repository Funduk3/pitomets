package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.exceptions.profileExceptions.ProfileAlreadyExistsException
import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.AdminMessage
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.UpdateUserProfileRequest
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.model.dto.response.UserWithProfilesResponse
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.model.entity.UserRole
import com.pitomets.monolit.model.entity.ListingOutbox
import com.pitomets.monolit.model.EventType
import com.pitomets.monolit.model.kafka.moderation.ModerationOperation
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.ReviewsRepo
import com.pitomets.monolit.repository.ListingPhotoRepo
import com.pitomets.monolit.repository.FavouritesRepo
import com.pitomets.monolit.repository.ListingOutboxRepository
import com.pitomets.monolit.repository.findUserOrThrow
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ProfileService(
    private val userRepo: UserRepo,
    private val sellerProfileRepo: SellerProfileRepo,
    private val listingsRepo: ListingsRepo,
    private val reviewsRepo: ReviewsRepo,
    private val listingPhotoRepo: ListingPhotoRepo,
    private val favouritesRepo: FavouritesRepo,
    private val listingOutboxRepo: ListingOutboxRepository,
    private val moderationRequestService: ModerationRequestService,
) {
    private val log = LoggerFactory.getLogger(ProfileService::class.java)

    @Transactional
    fun createSellerProfile(userId: Long, request: CreateSellerProfileRequest): SellerProfileResponse {
        val user = userRepo.findUserOrThrow(userId)

        if (user.sellerProfile != null) {
            throw ProfileAlreadyExistsException("Seller profile already exists for this user")
        }

        user.role = UserRole.SELLER

        val sellerProfile = SellerProfile(
            seller = user,
            shopName = request.shopName,
            description = request.description,
            isVerified = false,
            isApproved = false
        )
        val saved = sellerProfileRepo.save(sellerProfile)
        moderationRequestService.publishSellerProfile(saved, ModerationOperation.CREATE)

        updateSecurityContext(user)

        log.info("Seller profile created for user ID: {}, shop name: {}", userId, request.shopName)

        return SellerProfileResponse(
            id = requireNotNull(saved.id) { "Seller profile ID cannot be null" },
            userId = saved.seller?.id,
            shopName = saved.shopName,
            description = saved.description,
            rating = saved.rating,
            isVerified = saved.isVerified,
            createdAt = saved.createdAt,
            avatarKey = saved.seller?.avatarKey,
            moderationHint = moderationHint(
                status = saved.aiModerationStatus,
                reason = saved.aiModerationReason,
                toxicityScore = saved.aiToxicityScore,
                sourceAction = saved.aiSourceAction,
                modelVersion = saved.aiModelVersion
            )
        )
    }

    private fun updateSecurityContext(user: User) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.principal is UserPrincipal) {
            val newPrincipal = UserPrincipal(user)
            val newAuth = UsernamePasswordAuthenticationToken(
                newPrincipal,
                authentication.credentials,
                newPrincipal.authorities
            )
            newAuth.details = (authentication as? UsernamePasswordAuthenticationToken)?.details
            SecurityContextHolder.getContext().authentication = newAuth
        }
    }

    fun getUserWithProfiles(userId: Long): UserWithProfilesResponse {
        val user = userRepo.findUserOrThrow(userId)

        return UserWithProfilesResponse(
            id = requireNotNull(user.id),
            email = requireNotNull(user.email),
            fullName = user.fullName,
            isSeller = user.sellerProfile != null,
            sellerProfileId = user.sellerProfile?.id,
            shopName = user.sellerProfile?.shopName,
            description = user.sellerProfile?.description,
            rating = user.sellerProfile?.rating,
            isVerified = user.sellerProfile?.isVerified,
            sellerProfileApproved = user.sellerProfile?.isApproved,
            createdAt = user.sellerProfile?.createdAt,
            avatarKey = user.avatarKey,
            bannedUntil = user.bannedUntil,
            banMessage = user.bannedUntil?.takeIf { it.isAfter(OffsetDateTime.now()) }
                ?.let { "Ваш профиль был заблокирован модератором" },
            role = user.role.name,
        )
    }

    @Transactional
    fun updateUserProfile(userId: Long, request: UpdateUserProfileRequest): UserWithProfilesResponse {
        val user = userRepo.findUserOrThrow(userId)

        request.fullName?.let { user.fullName = it }

        userRepo.save(user)

        log.info("User profile updated for user ID: {}", userId)

        return getUserWithProfiles(userId)
    }

    @Transactional
    fun updateSellerProfile(userId: Long, request: CreateSellerProfileRequest): SellerProfileResponse {
        val user = userRepo.findUserOrThrow(userId)

        val sellerProfile = user.sellerProfile
            ?: throw UserNotFoundException("Seller profile not found for this user")

        sellerProfile.shopName = request.shopName
        sellerProfile.description = request.description
        sellerProfile.isApproved = false
        sellerProfile.isVerified = false

        val updated = sellerProfileRepo.save(sellerProfile)
        moderationRequestService.publishSellerProfile(updated, ModerationOperation.UPDATE)

        log.info("Seller profile updated for user ID: {}", userId)

        return SellerProfileResponse(
            id = requireNotNull(updated.id),
            userId = updated.seller?.id,
            shopName = updated.shopName,
            description = updated.description,
            rating = updated.rating,
            isVerified = updated.isVerified,
            createdAt = updated.createdAt,
            avatarKey = updated.seller?.avatarKey,
            moderationHint = moderationHint(
                status = updated.aiModerationStatus,
                reason = updated.aiModerationReason,
                toxicityScore = updated.aiToxicityScore,
                sourceAction = updated.aiSourceAction,
                modelVersion = updated.aiModelVersion
            )
        )
    }

    fun getSellerProfileByUserId(sellerId: Long): SellerProfileResponse {
        val sellerProfile = sellerProfileRepo.findBySellerId(sellerId)
            ?: throw UserNotFoundException("Seller profile not found for user ID: $sellerId")

        return SellerProfileResponse(
            id = requireNotNull(sellerProfile.id) { "Seller profile ID cannot be null" },
            userId = sellerProfile.seller?.id,
            shopName = sellerProfile.shopName,
            description = sellerProfile.description,
            rating = sellerProfile.rating,
            isVerified = sellerProfile.isVerified,
            createdAt = sellerProfile.createdAt,
            avatarKey = sellerProfile.seller?.avatarKey,
            moderationHint = moderationHint(
                status = sellerProfile.aiModerationStatus,
                reason = sellerProfile.aiModerationReason,
                toxicityScore = sellerProfile.aiToxicityScore,
                sourceAction = sellerProfile.aiSourceAction,
                modelVersion = sellerProfile.aiModelVersion
            )
        )
    }

    fun getSellerProfileByProfileId(sellerProfileId: Long): SellerProfileResponse {
        val sellerProfile = sellerProfileRepo.findById(sellerProfileId)
            .orElseThrow { UserNotFoundException("Seller profile not found for id: $sellerProfileId") }

        return SellerProfileResponse(
            id = requireNotNull(sellerProfile.id) { "Seller profile ID cannot be null" },
            userId = sellerProfile.seller?.id,
            shopName = sellerProfile.shopName,
            description = sellerProfile.description,
            rating = sellerProfile.rating,
            isVerified = sellerProfile.isVerified,
            createdAt = sellerProfile.createdAt,
            avatarKey = sellerProfile.seller?.avatarKey,
            moderationHint = moderationHint(
                status = sellerProfile.aiModerationStatus,
                reason = sellerProfile.aiModerationReason,
                toxicityScore = sellerProfile.aiToxicityScore,
                sourceAction = sellerProfile.aiSourceAction,
                modelVersion = sellerProfile.aiModelVersion
            )
        )
    }

    fun getPendingSellerProfiles(): List<SellerProfileResponse> {
        return sellerProfileRepo.findByIsApprovedFalse()
            .map { profile ->
                SellerProfileResponse(
                    id = requireNotNull(profile.id) { "Seller profile ID cannot be null" },
                    userId = profile.seller?.id,
                    shopName = profile.shopName,
                    description = profile.description,
                    rating = profile.rating,
                    isVerified = profile.isVerified,
                    createdAt = profile.createdAt,
                    avatarKey = profile.seller?.avatarKey,
                    moderationHint = moderationHint(
                        status = profile.aiModerationStatus,
                        reason = profile.aiModerationReason,
                        toxicityScore = profile.aiToxicityScore,
                        sourceAction = profile.aiSourceAction,
                        modelVersion = profile.aiModelVersion
                    )
                )
            }
    }

    fun getPendingSellerProfile(id: Long): SellerProfileResponse {
        val profile = sellerProfileRepo.findByIdAndIsApprovedFalse(id)
            ?: throw UserNotFoundException("Pending seller profile with id $id not found")
        return SellerProfileResponse(
            id = requireNotNull(profile.id) { "Seller profile ID cannot be null" },
            userId = profile.seller?.id,
            shopName = profile.shopName,
            description = profile.description,
            rating = profile.rating,
            isVerified = profile.isVerified,
            createdAt = profile.createdAt,
            avatarKey = profile.seller?.avatarKey,
            moderationHint = moderationHint(
                status = profile.aiModerationStatus,
                reason = profile.aiModerationReason,
                toxicityScore = profile.aiToxicityScore,
                sourceAction = profile.aiSourceAction,
                modelVersion = profile.aiModelVersion
            )
        )
    }

    @Transactional
    fun approveSellerProfile(id: Long) {
        val profile = sellerProfileRepo.findByIdAndIsApprovedFalse(id)
            ?: throw UserNotFoundException("Pending seller profile with id $id not found")
        profile.isApproved = true
        profile.isVerified = true
        sellerProfileRepo.save(profile)
    }

    @Transactional
    fun declineSellerProfile(id: Long, adminMessage: AdminMessage) {
        val profile = sellerProfileRepo.findByIdAndIsApprovedFalse(id)
            ?: throw UserNotFoundException("Pending seller profile with id $id not found")
        val seller = profile.seller
        log.info("Declining seller profile id={} reason={}", id, adminMessage.message)
        val listings = listingsRepo.findBySellerProfile(profile)
        listings.forEach { listing ->
            val listingId = requireNotNull(listing.id)
            reviewsRepo.deleteByListingId(listingId)
            listingPhotoRepo.deleteAllByListingId(listingId)
            favouritesRepo.deleteAllByListingId(listingId)
            listingOutboxRepo.save(
                ListingOutbox(
                    listingId = listingId,
                    eventType = EventType.DELETE,
                    title = null,
                    description = null,
                    city = 0,
                    metro = null,
                    price = 0.toBigDecimal(),
                )
            )
        }
        listingsRepo.deleteAll(listings)
        reviewsRepo.deleteBySellerProfileId(requireNotNull(profile.id))

        seller?.let { user ->
            user.role = UserRole.USER
            user.sellerProfile = null
            user.bannedUntil = OffsetDateTime.now().plusDays(BAN_DAYS)
            userRepo.save(user)
        }
        sellerProfileRepo.delete(profile)
    }

    companion object {
        private const val BAN_DAYS: Long = 7
    }
}
