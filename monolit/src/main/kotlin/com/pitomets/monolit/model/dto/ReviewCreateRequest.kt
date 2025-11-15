package com.pitomets.monolit.model.dto

data class ReviewCreateRequest(
    val reviewerId: Long,
    val sellerProfileId: Long,
    val starsNumber: Short,
    val message: String?
)