package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.IllegalPasswordException
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
import com.pitomets.monolit.repository.findUserOrThrow
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
            ?: throw IllegalPasswordException("Problem with password")
        user.role = UserRole.USER

        val token = java.util.UUID.randomUUID().toString()
        user.confirmationToken = token

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
                messageType = "CONFIRM",
                payload = "${savedUser.email} $token",
            )
        )
        log.info("KAFKA REGISTRATION MESSAGE SENT")

        return UserResponse(
            id = requireNotNull(savedUser.id) { "User ID cannot be null" },
            email = savedUser.email,
            fullName = savedUser.fullName,
            hasBuyerProfile = true,
            hasSellerProfile = false,
            message = "На вашу почту отправлено письмо с подтверждением," +
                    " проверьте свой почтовый ящик и перейдите по ссылке, чтобы подтвердить почту"
        )
    }

    @Suppress("ThrowsCount")
    fun login(email: String, rawPassword: String): TokenResponse {
        try {
            val authToken = UsernamePasswordAuthenticationToken(email, rawPassword)
            val authentication: Authentication = authManager.authenticate(authToken)

            if (authentication.isAuthenticated) {
                val user = repo.findByEmail(email)
                    ?: throw UserNotFoundException("User not found")
                if (!user.isConfirmed) {
                    log.warn("Login blocked: email not confirmed for user {}", email)
                    throw AuthenticationException("подтвердите электронную почту")
                }

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

    @Transactional
    fun confirmEmail(token: String) {
        log.info("Confirm email requested with token={}", token)
        val user = repo.findByConfirmationToken(token)
        if (user == null) {
            log.warn("Confirm email failed: no user found for token={}", token)
            throw InvalidTokenException("Invalid confirmation token")
        }
        log.info("Confirm email found user id={} email={}", user.id, user.email)
        user.isConfirmed = true
        user.confirmationToken = null
        repo.save(user)
        log.info("Confirm email success for user id={}", user.id)
    }


    fun forgotPassword(email: String) {
        val user = repo.findByEmail(email)
        if (user != null) {
            val token = java.util.UUID.randomUUID().toString()
            user.passwordResetToken = token
            repo.save(user)

            val eventId = System.currentTimeMillis()
            notificationPublisher.publish(
                NotificationRequestedEvent(
                    eventId = eventId,
                    userId = requireNotNull(user.id),
                    channel = Channel.EMAIL,
                    messageType = "RESTORE_PASSWORD",
                    payload = "${user.email} $token",
                )
            )
            log.info("KAFKA restore password message sent")

        }
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String, confirmPassword: String) {
        if (newPassword != confirmPassword) {
            throw IllegalArgumentException("Passwords do not match")
        }
        val user = repo.findByPasswordResetToken(token)
            ?: throw InvalidTokenException("Invalid reset token")

        user.passwordHash = encoder.encode(newPassword)
            ?: throw IllegalPasswordException("Problem with password")
        user.passwordResetToken = null
        repo.save(user)
    }

    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String, confirmPassword: String) {
        if (newPassword != confirmPassword) {
            throw IllegalArgumentException("Passwords do not match")
        }

        val user = repo.findUserOrThrow(userId)
        if (!encoder.matches(currentPassword, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid current password")
        }

        user.passwordHash = encoder.encode(newPassword)
            ?: throw IllegalPasswordException("Problem with password")
        repo.save(user)
    }
}
