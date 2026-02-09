package com.pitomets.monolit.model.dto.response

data class ListingsCursorResponse(
    val items: List<ListingsResponse>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
