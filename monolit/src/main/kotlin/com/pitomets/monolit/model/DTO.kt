package com.pitomets.monolit.model

data class RegisterRequest(val name: String, val password: String)
data class LoginRequest(val name: String, val password: String)
data class LoginResponse(val token: String)
data class UserResponse(val id: Long?, val name: String)