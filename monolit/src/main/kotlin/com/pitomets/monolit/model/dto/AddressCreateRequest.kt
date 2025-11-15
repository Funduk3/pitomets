package com.pitomets.monolit.model.dto

data class AddressCreateRequest(
    val userId: Long,
    val city: String,
    val street: String,
    val house: String,
    val flat: Int
)