package com.pitomets.monolit.model.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer"
)

data class RefreshTokenRequest(
    val refreshToken: String
)