package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.authExceptions.AuthenticationException
import com.pitomets.monolit.exceptions.InvalidCredentialsException
import com.pitomets.monolit.exceptions.authExceptions.InvalidTokenException
import com.pitomets.monolit.exceptions.UserAlreadyExistsException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.dto.TokenResponse
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.repository.UserRepo
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val jwtService: JWTService,
    private val authManager: AuthenticationManager,
    private val repo: UserRepo,
    private val encoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun register(user: User): User {
        if (repo.findByFullName(user.fullName) != null) {
            throw UserAlreadyExistsException("User with this name already exists")
        }
        user.passwordHash = encoder.encode(user.passwordHash)
        val savedUser = repo.save(user)
        savedUser.passwordHash = ""
        return savedUser
    }

    fun login(name: String, rawPassword: String): TokenResponse {
        try {
            val authToken = UsernamePasswordAuthenticationToken(name, rawPassword)
            val authentication: Authentication = authManager.authenticate(authToken)

            if (authentication.isAuthenticated) {
                val accessToken = jwtService.generateAccessToken(name)
                val refreshToken = jwtService.createRefreshToken(name)

                return TokenResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    tokenType = "Bearer"
                )
            }
        } catch (ex: BadCredentialsException) {
            log.warn("Authentication failed for user {}: {}", name, ex.message)
            throw InvalidCredentialsException("Invalid username or password")
        } catch (ex: Exception) {
            log.warn("Authentication failed for user {}: {}", name, ex.message)
            throw AuthenticationException("Authentication failed")
        }
        throw AuthenticationException("Authentication failed")
    }

    fun refreshAccessToken(refreshToken: String): TokenResponse {
        val username = jwtService.consumeRefreshToken(refreshToken)
            ?: throw InvalidTokenException("Invalid or expired refresh token")

        repo.findByFullName(username)
            ?: throw UserNotFoundException("User not found")

        val newAccessToken = jwtService.generateAccessToken(username)
        val newRefreshToken = jwtService.createRefreshToken(username)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer" // todo fix
        )
    }

    fun logout(refreshToken: String) {
        try {
            jwtService.deleteRefreshToken(refreshToken)
        } catch (ex: Exception) {
            throw InvalidTokenException("Invalid refresh token")
        }
    }

    // для теста
    fun getAll(): List<User> {
        val users = repo.findAll()
        users.forEach { it.passwordHash = "" }
        return users
    }
}
