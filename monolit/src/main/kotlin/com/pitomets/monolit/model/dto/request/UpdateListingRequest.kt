package com.pitomets.monolit.model.dto.request

import com.pitomets.monolit.model.entity.Pet
import java.math.BigDecimal

data class UpdateListingRequest(
    val description: String?,

    val species: String?,

    val breed: String?,

    val ageMonths: Int?,

    val mother: Pet?,

    val father: Pet?,

    val price: BigDecimal?,

    val isArchived: Boolean?,
)
