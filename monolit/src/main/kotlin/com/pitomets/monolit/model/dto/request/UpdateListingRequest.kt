package com.pitomets.monolit.model.dto.request

import java.math.BigDecimal

data class UpdateListingRequest(
    val description: String?,

    val species: String?,

    val breed: String?,

    val ageMonths: Int?,

    val mother: Long?,

    val father: Long?,

    val price: BigDecimal?,

    val isArchived: Boolean?,

    val title: String?,
)
