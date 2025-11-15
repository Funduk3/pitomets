package com.pitomets.monolit.model.dto

data class LoginRequest(
    val fullName: String,
    val passwordHash: String
)