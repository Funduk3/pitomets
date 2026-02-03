package com.pitomets.monolit.model.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
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
    @JsonProperty("verified")
    @JsonAlias("isVerified")
    val isVerified: Boolean?,
    val createdAt: OffsetDateTime?,

    // avatar
    val avatarKey: String?,
)
