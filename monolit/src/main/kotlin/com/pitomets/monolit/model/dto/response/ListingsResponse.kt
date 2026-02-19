package com.pitomets.monolit.model.dto.response

import com.pitomets.monolit.model.Gender
import java.math.BigDecimal

data class ListingsResponse(
    val listingsId: Long,

    val description: String,

    val sellerId: Long,

    val sellerRating: Double?,

    val sellerReviewsCount: Long?,

    val coverPhotoId: Long?,

    val species: String?,

    val breed: String?,

    val ageMonths: Int,

    val gender: Gender?,

    val price: BigDecimal,

    val isArchived: Boolean,

    val mother: Long?,

    val father: Long?,

    val title: String,

    val city: CityDto,

    val metro: MetroDto?,

    val viewsCount: Long,

    val likesCount: Long,

    val moderatorMessage: String? = null
)
