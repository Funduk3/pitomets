package com.pitomets.monolit.model.dto.response

data class SearchListingsPageResponse(
    val items: List<SearchListingsResponse>,
    val nextSearchAfter: List<Any>?
)
