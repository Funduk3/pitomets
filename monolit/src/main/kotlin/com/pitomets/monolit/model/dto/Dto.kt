package com.pitomets.monolit.model.dto

import java.time.OffsetDateTime

data class RegisterRequest(
    val fullName: String,
    val passwordHash: String
)

data class LoginRequest(
    val fullName: String,
    val passwordHash: String
)

data class LoginResponse(
    val token: String
)

data class UserResponse(
    val id: Long,
    val fullName: String
)


// User
//data class RegisterRequest(val email: String, val password: String, val fullName: String? = null)
//data class LoginRequest(val email: String, val password: String)
//data class UserResponse(val id: Long, val email: String, val fullName: String?, val createdAt: OffsetDateTime?)

// SellerProfile
data class SellerProfileCreateRequest(val sellerId: Long, val shopName: String?, val description: String?)
data class SellerProfileResponse(val id: Long, val sellerId: Long, val shopName: String?, val description: String?, val rating: Int?)

// Buyer / Admin profiles
data class BuyerProfileResponse(val id: Long, val buyerId: Long)
data class AdminProfileResponse(val id: Long, val adminId: Long)

// Pet
data class PetCreateRequest(val species: String?, val breed: String?, val age: Int?, val weight: Double?, val gender: String?, val description: String?)
data class PetResponse(val id: Long, val species: String?, val breed: String?, val age: Int?, val weight: Double?, val gender: String?, val description: String?)

// Listing
data class ListingCreateRequest(
    val sellerProfileId: Long,
    val description: String?,
    val species: String?,
    val breed: String?,
    val ageMonths: Int?,
    val motherId: Long?,
    val fatherId: Long?,
    val price: Int?,
    val isArchived: Boolean = false
)
data class ListingResponse(
    val id: Long,
    val sellerProfileId: Long?,
    val description: String?,
    val species: String?,
    val breed: String?,
    val ageMonths: Int?,
    val motherId: Long?,
    val fatherId: Long?,
    val price: Int?,
    val isArchived: Boolean?
)

// AdminAction
data class AdminActionCreateRequest(val adminId: Long, val sellerProfileId: Long?, val reason: String?, val type: String?)
data class AdminActionResponse(val id: Long, val adminId: Long, val sellerProfileId: Long?, val reason: String?, val type: String?, val actedAt: OffsetDateTime?)

// Address
data class AddressCreateRequest(val userId: Long, val city: String?, val street: String?, val house: String?, val flat: Int?)
data class AddressResponse(val id: Long, val userId: Long, val city: String?, val street: String?, val house: String?, val flat: Int?)

// Favourite
data class FavouriteRequest(val buyerProfileId: Long, val listingId: Long)
data class FavouriteResponse(val buyerProfileId: Long, val listingId: Long)

// Review
data class ReviewCreateRequest(val reviewerId: Long, val sellerProfileId: Long, val starsNumber: Short, val message: String?)
data class ReviewResponse(val id: Long, val reviewerId: Long, val sellerProfileId: Long, val starsNumber: Short, val message: String?)
