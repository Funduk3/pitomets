package com.pitomets.monolit.model.dto

data class ListingResponse(
    val id: Long,
    val sellerProfileId: Long?,
    val description: String?,
    val species: String?,
    val breed: String?,
    val ageMonths: Int?,
    val motherId: Long?,
    val fatherId: Long?,
    val price: Int?,
    val isArchived: Boolean?
)