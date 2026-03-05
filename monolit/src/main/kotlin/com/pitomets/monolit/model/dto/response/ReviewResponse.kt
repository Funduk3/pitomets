package com.pitomets.monolit.model.dto.response

import java.time.OffsetDateTime

data class ReviewResponse(
    val id: Long,
    val rating: Int,
    val text: String?,
    val authorId: Long,
    val listingId: Long,
    val sellerProfileId: Long,
    val createdAt: OffsetDateTime,
    val moderationHint: ModerationHintResponse? = null,
)
