package com.pitomets.monolit.model.dto.request

import java.math.BigDecimal

data class ListingsRequest(
    val description: String,

    val species: String,

    val breed: String,

    val ageMonths: Int,

    val mother: Long? = null,

    val father: Long? = null,

    val price: BigDecimal,
)
