package com.pitomets.monolit.model.dto

data class PetResponse(
    val id: Long,
    val species: String?,
    val breed: String?,
    val age: Int?,
    val weight: Double?,
    val gender: String?,
    val description: String?
)