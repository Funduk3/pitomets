package com.pitomets.monolit.model.dto

data class AddressResponse(
    val id: Long,
    val userId: Long,
    val city: String,
    val street: String,
    val house: String,
    val flat: Int
)