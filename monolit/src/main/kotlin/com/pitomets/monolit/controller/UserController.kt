package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.request.LoginRequest
import com.pitomets.monolit.model.dto.request.RefreshTokenRequest
import com.pitomets.monolit.model.dto.request.RegisterRequest
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.model.dto.response.UserResponse
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val service: UserService
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest): UserResponse =
        service.register(
            User(
                email = request.email,
                fullName = request.fullName,
                passwordHash = request.passwordHash
            )
        )

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): TokenResponse =
        service.login(
            request.email,
            request.passwordHash
        )

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): TokenResponse =
        service.refreshAccessToken(request.refreshToken)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    fun logout(@RequestBody request: RefreshTokenRequest) =
        service.logout(request.refreshToken)

    @GetMapping("/all")
    fun getAll(): List<UserResponse> =
        service.getAll()
}
