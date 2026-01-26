package com.pitomets.monolit.model.dto.request

data class UpdateListingReviewRequest(
    var rating: Int,

    var text: String? = null,

    var authorId: Long,

    var listingId: Long,
)
