package com.pitomets.monolit.model.dto.request

data class SearchListingsRequest(
    val query: String,
    val page: Int = 0,
    val size: Int = 10,
)
