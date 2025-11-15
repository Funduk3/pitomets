package com.pitomets.monolit.model.dto

data class SellerProfileResponse(
    val id: Long,
    val sellerId: Long,
    val shopName: String?,
    val description: String?,
    val rating: Int?
)