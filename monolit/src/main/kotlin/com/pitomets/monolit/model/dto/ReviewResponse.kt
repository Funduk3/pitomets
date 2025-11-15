package com.pitomets.monolit.model.dto

data class ReviewResponse(
    val id: Long,
    val reviewerId: Long,
    val sellerProfileId: Long,
    val starsNumber: Short,
    val message: String?
)