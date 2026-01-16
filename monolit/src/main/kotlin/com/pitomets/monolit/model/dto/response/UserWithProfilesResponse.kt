package com.pitomets.monolit.model.dto.response

import java.time.OffsetDateTime

data class UserWithProfilesResponse(
    val id: Long,
    val email: String,
    val fullName: String,

    val isSeller: Boolean,

    // seller
    val shopName: String?,
    val description: String?,
    val rating: Double?,
    val isVerified: Boolean?,
    val createdAt: OffsetDateTime?,
    
    // avatar
    val avatarKey: String?,
)
