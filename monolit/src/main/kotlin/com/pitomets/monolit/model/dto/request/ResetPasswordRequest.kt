package com.pitomets.monolit.model.dto.request

data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)
