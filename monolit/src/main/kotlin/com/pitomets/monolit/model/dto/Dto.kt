package com.pitomets.monolit.model.dto

data class RegisterRequest(
    val fullName: String,
    val passwordHash: String
)

data class LoginRequest(
    val fullName: String,
    val passwordHash: String
)

data class LoginResponse(
    val token: String
)

data class UserResponse(
    val id: Long,
    val fullName: String
)