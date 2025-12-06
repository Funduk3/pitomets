package com.pitomets.monolit.model.dto.response

import com.pitomets.monolit.model.entity.Pet
import java.math.BigDecimal

data class ListingsResponse(
    val listingsId: Long,

    val description: String,

    val species: String?,

    val breed: String?,

    val ageMonths: Int,

    val mother: Pet?,

    val father: Pet?,

    val price: BigDecimal,

    val isArchived: Boolean,
)
