package com.pitomets.monolit.service

import com.pitomets.monolit.model.User
import com.pitomets.monolit.repository.UserRepo
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service


@Service
class UserService(
    private val jwtService: JWTService? = null,
    var authManager: AuthenticationManager? = null,
    private val repo: UserRepo? = null,
) {
    private val encoder = BCryptPasswordEncoder(12)

    fun register(user: User): User {
        user.password = encoder.encode(user.password)
        repo!!.save<User>(user)
        return user
    }

    fun verify(user: User): String? {
        val authentication: Authentication =
            authManager!!.authenticate(UsernamePasswordAuthenticationToken(user.name, user.password))
        return if (authentication.isAuthenticated) {
            jwtService?.generateToken(user.name)
        } else {
            "fail"
        }
    }
}