package com.pitomets.monolit.model.dto.response

data class UserWithProfilesResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val sellerProfile: SellerProfileResponse?,
    val buyerProfile: BuyerProfileResponse?
)
