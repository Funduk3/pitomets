package com.pitomets.monolit.model.dto.response

import java.math.BigDecimal

data class ListingsResponse(
    val listingsId: Long,

    val description: String,

    val species: String?,

    val breed: String?,

    val ageMonths: Int,

    val price: BigDecimal,

    val isArchived: Boolean,

    val mother: Long?,

    val father: Long?,
)
