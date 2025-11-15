package com.pitomets.monolit.model.dto

data class RegisterRequest(
    val fullName: String,
    val passwordHash: String
)