package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.exceptions.profileExceptions.ProfileAlreadyExistsException
import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.UpdateUserProfileRequest
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.model.dto.response.UserWithProfilesResponse
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.model.entity.UserRole
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import com.pitomets.monolit.repository.findUserOrThrow
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(
    private val userRepo: UserRepo,
    private val sellerProfileRepo: SellerProfileRepo
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
            description = request.description
        )
        val saved = sellerProfileRepo.save(sellerProfile)

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
            avatarKey = saved.seller?.avatarKey
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
            createdAt = user.sellerProfile?.createdAt,
            avatarKey = user.avatarKey,
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

        val updated = sellerProfileRepo.save(sellerProfile)

        log.info("Seller profile updated for user ID: {}", userId)

        return SellerProfileResponse(
            id = requireNotNull(updated.id),
            userId = updated.seller?.id,
            shopName = updated.shopName,
            description = updated.description,
            rating = updated.rating,
            isVerified = updated.isVerified,
            createdAt = updated.createdAt,
            avatarKey = updated.seller?.avatarKey
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
            avatarKey = sellerProfile.seller?.avatarKey
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
            avatarKey = sellerProfile.seller?.avatarKey
        )
    }
}
