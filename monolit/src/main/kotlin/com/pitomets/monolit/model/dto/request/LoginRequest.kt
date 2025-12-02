package com.pitomets.monolit.model.dto.request

data class LoginRequest(
    val email: String,
    val passwordHash: String
)
