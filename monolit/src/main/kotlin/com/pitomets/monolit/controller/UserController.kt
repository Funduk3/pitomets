package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.ChangePasswordRequest
import com.pitomets.monolit.model.dto.request.LoginRequest
import com.pitomets.monolit.model.dto.request.RefreshTokenRequest
import com.pitomets.monolit.model.dto.request.RegisterRequest
import com.pitomets.monolit.model.dto.request.ResetPasswordRequest
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.model.dto.response.UserResponse
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.annotation.AuthenticationPrincipal

@RestController
class UserController(
    private val service: UserService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody request: RegisterRequest
    ): UserResponse =
        service.register(
            User(
                email = request.email,
                fullName = request.fullName,
                passwordHash = request.passwordHash
            )
        )

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest
    ): TokenResponse =
        service.login(
            request.email,
            request.passwordHash
        )

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshTokenRequest
    ): TokenResponse =
        service.refreshAccessToken(request.refreshToken)

    @PostMapping("/logout")
    fun logout(
        @RequestParam refreshToken: String
    ) = service.logout(refreshToken)

    @GetMapping("/confirm")
    fun confirm(@RequestParam token: String): TokenResponse =
        service.confirmEmail(token)

    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody request: com.pitomets.monolit.model.dto.request.ForgotPasswordRequest) {
        service.forgotPassword(request.email)
    }

    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody request: ResetPasswordRequest) {
        service.resetPassword(request.token, request.newPassword, request.confirmPassword)
    }

    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: ChangePasswordRequest
    ) {
        service.changePassword(
            userId = userPrincipal.id,
            currentPassword = request.currentPassword,
            newPassword = request.newPassword,
            confirmPassword = request.confirmPassword
        )
    }
}
