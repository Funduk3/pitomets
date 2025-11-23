package com.pitomets.monolit.model.dto.request

data class RegisterRequest(
    val fullName: String,
    val passwordHash: String,
)
