package com.pitomets.monolit.model.dto.request

data class CreateSellerProfileRequest(
    val shopName: String,
    val description: String? = null
)
