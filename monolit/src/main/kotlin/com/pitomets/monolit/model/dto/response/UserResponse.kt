package com.pitomets.monolit.model.dto.response

data class UserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val hasBuyerProfile: Boolean,
    val hasSellerProfile: Boolean,
    val message: String? = null
)
