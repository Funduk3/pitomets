package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.InvalidCredentialsException
import com.pitomets.monolit.exceptions.UserAlreadyExistsException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.exceptions.authExceptions.AuthenticationException
import com.pitomets.monolit.exceptions.authExceptions.InvalidTokenException
import com.pitomets.monolit.kafka.notifications.producer.NotificationPublisher
import com.pitomets.monolit.model.dto.response.TokenResponse
import com.pitomets.monolit.model.dto.response.UserResponse
import com.pitomets.monolit.model.entity.BuyerProfile
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.model.entity.UserRole
import com.pitomets.monolit.model.kafka.NotificationRequestedEvent
import com.pitomets.monolit.model.kafka.event.Channel
import com.pitomets.monolit.repository.BuyerProfileRepo
import com.pitomets.monolit.repository.UserRepo
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val jwtService: JWTService,
    private val notificationPublisher: NotificationPublisher,
    private val authManager: AuthenticationManager,
    private val repo: UserRepo,
    private val buyerProfileRepo: BuyerProfileRepo,
    private val encoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    @Transactional
    fun register(user: User): UserResponse {
        if (repo.findByEmail(user.email) != null) {
            throw UserAlreadyExistsException("User with this email already exists")
        }
        user.passwordHash = encoder.encode(user.passwordHash)
        user.role = UserRole.USER

        val savedUser = repo.save(user)
        val buyerProfile = BuyerProfile(
            buyer = savedUser
        )
        buyerProfileRepo.save(buyerProfile)

        log.info("User registered with email: {} and buyer profile created", savedUser.email)

        val eventId = System.currentTimeMillis()

        notificationPublisher.publish(
            NotificationRequestedEvent(
                eventId = eventId,
                userId = requireNotNull(savedUser.id) { "User with this user doesn't have a id" },
                channel = Channel.EMAIL,
                payload = savedUser.email,
            )
        )
        /*
            чтобы было видно в логах, дев версия
         */
        log.info("KAFKA MESSAGE SENT")
        log.info("KAFKA MESSAGE SENT")
        log.info("KAFKA MESSAGE SENT")
        log.info("KAFKA MESSAGE SENT")
        log.info("KAFKA MESSAGE SENT")

        return UserResponse(
            id = requireNotNull(savedUser.id) { "User ID cannot be null" },
            email = savedUser.email,
            fullName = savedUser.fullName,
            hasBuyerProfile = true,
            hasSellerProfile = false
        )
    }

    fun login(email: String, rawPassword: String): TokenResponse {
        try {
            val authToken = UsernamePasswordAuthenticationToken(email, rawPassword)
            val authentication: Authentication = authManager.authenticate(authToken)

            if (authentication.isAuthenticated) {
                val accessToken = jwtService.generateAccessToken(email)
                val refreshToken = jwtService.createRefreshToken(email)

                return TokenResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                )
            }
        } catch (ex: BadCredentialsException) { // todo посмотри Федя можно ли удалить это
            log.warn("Authentication failed for user {}: {}", email, ex.message)
            throw InvalidCredentialsException("Invalid email or password")
        }
        throw AuthenticationException("Authentication failed")
    }

    fun refreshAccessToken(refreshToken: String): TokenResponse {
        val email = jwtService.consumeRefreshToken(refreshToken)
            ?: throw InvalidTokenException("Invalid or expired refresh token")

        repo.findByEmail(email)
            ?: throw UserNotFoundException("User not found")

        val newAccessToken = jwtService.generateAccessToken(email)
        val newRefreshToken = jwtService.createRefreshToken(email)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    fun logout(refreshToken: String) {
        jwtService.deleteRefreshToken(refreshToken)
    }

    fun getAll(): List<UserResponse> {
        val users = repo.findAll()
        return users.map {
            UserResponse(
                id = requireNotNull(it.id) { "User ID cannot be null" },
                email = it.email,
                fullName = it.fullName,
                hasBuyerProfile = it.buyerProfile != null,
                hasSellerProfile = it.sellerProfile != null
            )
        }
    }
}
