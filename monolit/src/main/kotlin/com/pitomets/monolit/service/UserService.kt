package com.pitomets.monolit.service

import com.pitomets.monolit.model.User
import com.pitomets.monolit.repository.UserRepo
import org.springframework.security.authentication.AuthenticationManager
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

    fun register(user: User): User {
        if (repo.findByName(user.name) != null) {
            throw IllegalArgumentException("User with this name already exists")
        }

        val hashedPassword = encoder.encode(user.password)
        val savedUser = repo.save(user.copy(password = hashedPassword))

        return savedUser.copy(password = "")
    }

    fun verify(name: String, rawPassword: String): String {
        try {
            val authToken = UsernamePasswordAuthenticationToken(name, rawPassword)
            val authentication: Authentication = authManager.authenticate(authToken)

            if (authentication.isAuthenticated) {
                return jwtService.generateToken(name)
            }
        } catch (ex: Exception) {
            // Можно логировать
        }
        throw IllegalArgumentException("Invalid username or password")
    }

    // для теста
    fun getAll(): List<User> {
        return repo.findAll().map { it.copy(password = "") }
    }
}
