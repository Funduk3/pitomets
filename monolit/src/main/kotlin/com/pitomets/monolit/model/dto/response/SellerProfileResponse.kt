package com.pitomets.monolit.model.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class SellerProfileResponse(
    val id: Long,
    val userId: Long?,
    val shopName: String,
    val description: String?,
    val rating: Double?,
    @JsonProperty("verified")
    @JsonAlias("isVerified")
    val isVerified: Boolean,
    val createdAt: OffsetDateTime,
    val avatarKey: String?,
    val moderationHint: ModerationHintResponse? = null,
    val photoModerationHint: PhotoModerationHintResponse? = null
)
