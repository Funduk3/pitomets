package com.pitomets.monolit.model.dto.response

import java.math.BigDecimal

data class SearchListingsResponse(
    val id: Long,
    val title: String,
    val description: String,
    val price: BigDecimal,
    val cityTitle: String?,
    val viewsCount: Long? = null,
    val likesCount: Long? = null,
)
