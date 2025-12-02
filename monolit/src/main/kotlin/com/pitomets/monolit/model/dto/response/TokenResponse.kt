package com.pitomets.monolit.model.dto.response

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
