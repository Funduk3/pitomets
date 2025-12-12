package com.pitomets.monolit.model.dto.request

data class CreateReviewRequest(
    val listingId: Long,
    val sellerProfileId: Long,
    val rating: Int,
    val text: String? = null,
)
