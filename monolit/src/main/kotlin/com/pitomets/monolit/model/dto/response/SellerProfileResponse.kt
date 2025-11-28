package com.pitomets.monolit.model.dto.response

import java.time.OffsetDateTime

data class SellerProfileResponse(
    val id: Long,
    val shopName: String,
    val description: String?,
    val rating: Double?,
    val isVerified: Boolean,
    val createdAt: OffsetDateTime
)
