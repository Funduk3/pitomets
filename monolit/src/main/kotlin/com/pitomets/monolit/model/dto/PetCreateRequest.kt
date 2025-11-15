package com.pitomets.monolit.model.dto

data class PetCreateRequest(
    val species: String?,
    val breed: String?,
    val age: Int?,
    val weight: Double?,
    val gender: String?,
    val description: String?
)