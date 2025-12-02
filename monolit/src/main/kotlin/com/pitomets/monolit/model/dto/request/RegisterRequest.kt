package com.pitomets.monolit.model.dto.request

data class RegisterRequest(
    val email: String,
    val passwordHash: String,
    val fullName: String
)
