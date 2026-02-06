package com.pitomets.monolit.model.dto.request

import com.pitomets.monolit.model.Gender
import java.math.BigDecimal

data class UpdateListingRequest(
    val description: String?,

    val species: String?,

    val breed: String?,

    val ageMonths: Int?,

    val gender: Gender?,

    val mother: Long?,

    val father: Long?,

    val price: BigDecimal?,

    val isArchived: Boolean?,

    val title: String?,

    val city: Long?,

    val metroStation: Long?,
)
