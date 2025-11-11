package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.*
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
    private val service: UserService
) {
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        return try {
            val user = User(fullName = request.fullName, passwordHash = request.passwordHash, email = "123")
            val savedUser = service.register(user)
            ResponseEntity(
                UserResponse(id = savedUser.id!!, fullName = savedUser.fullName),
                HttpStatus.CREATED
            )
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        return try {
            val tokens = service.login(request.fullName, request.passwordHash)
            ResponseEntity.ok(tokens)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): ResponseEntity<Any> {
        return try {
            val tokens = service.refreshAccessToken(request.refreshToken)
            ResponseEntity.ok(tokens)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(401).body(
                mapOf(
                    "status" to 401,
                    "error" to "Unauthorized",
                    "message" to ex.message
                )
            )
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: RefreshTokenRequest): ResponseEntity<Void> {
        return try {
            service.logout(request.refreshToken)
            ResponseEntity.ok().build()
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @PostMapping("/logout-all")
    fun logoutAll(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Void> {
        return try {
            service.logoutAll(userDetails.username)
            ResponseEntity.ok().build()
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @GetMapping("/all")
    fun getAll(): List<UserResponse> {
        return service.getAll().map { UserResponse(it.id!!, it.fullName) }
    }
}