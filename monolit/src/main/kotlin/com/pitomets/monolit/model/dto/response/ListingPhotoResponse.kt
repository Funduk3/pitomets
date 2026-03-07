package com.pitomets.monolit.model.dto.response

data class ListingPhotoResponse(
    val title: String,
    val photos: List<String>,
    val photoIds: List<Long> = emptyList(),
)
