package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.exceptions.profileExceptions.ProfileAlreadyExistsException
import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.response.BuyerProfileResponse
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.model.dto.response.UserWithProfilesResponse
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.model.entity.UserRole
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.UserRepo
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
        val user = userRepo.findById(userId).orElseThrow {
            UserNotFoundException("User not found")
        }
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
            shopName = saved.shopName,
            description = saved.description,
            rating = saved.rating,
            isVerified = saved.isVerified,
            createdAt = saved.createdAt
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
        val user = userRepo.findById(userId).orElseThrow {
            UserNotFoundException("User not found")
        }

        return UserWithProfilesResponse(
            id = user.id!!, // fix
            email = user.email!!, // fix
            fullName = user.fullName,
            sellerProfile = user.sellerProfile?.let {
                it.id?.let { it1 ->
                    SellerProfileResponse(
                        id = it1,
                        shopName = it.shopName,
                        description = it.description,
                        rating = it.rating,
                        isVerified = it.isVerified,
                        createdAt = it.createdAt
                    )
                }
            },
            buyerProfile = user.buyerProfile?.let {
                it.id?.let { it1 ->
                    BuyerProfileResponse(
                        id = it1,
                    )
                }
            }
        )
    }

    @Transactional
    fun updateSellerProfile(userId: Long, request: CreateSellerProfileRequest): SellerProfileResponse {
        val user = userRepo.findById(userId).orElseThrow {
            UserNotFoundException("User not found")
        }

        val sellerProfile = user.sellerProfile
            ?: throw UserNotFoundException("Seller profile not found for this user")

        sellerProfile.shopName = request.shopName
        sellerProfile.description = request.description

        val updated = sellerProfileRepo.save(sellerProfile)

        log.info("Seller profile updated for user ID: {}", userId)

        return SellerProfileResponse(
            id = updated.id!!,
            shopName = updated.shopName,
            description = updated.description,
            rating = updated.rating,
            isVerified = updated.isVerified,
            createdAt = updated.createdAt
        )
    }
}
