package com.pitomets.monolit.model.dto

data class SellerProfileCreateRequest(
    val sellerId: Long,
    val shopName: String,
    val description: String?
)