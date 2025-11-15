package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.*
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
    private val service: UserService
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest): UserResponse {
        val user = User(fullName = request.fullName, passwordHash = request.passwordHash, email = "123")
        val savedUser = service.register(user)
        return UserResponse(id = savedUser.id!!, fullName = savedUser.fullName)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): TokenResponse {
        return service.login(request.fullName, request.passwordHash)
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): TokenResponse =
        service.refreshAccessToken(request.refreshToken)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    fun logout(@RequestBody request: RefreshTokenRequest) {
        service.logout(request.refreshToken)
    }

    @GetMapping("/all")
    fun getAll(): List<UserResponse> {
        return service.getAll().map { UserResponse(it.id!!, it.fullName) }
    }
}