package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.jwtException.JWTException
import com.pitomets.monolit.exceptions.jwtException.JWTExpiredException
import com.pitomets.monolit.exceptions.jwtException.JWTMalformedException
import com.pitomets.monolit.exceptions.jwtException.JWTValidationException
import com.pitomets.monolit.exceptions.refreshTokenException.RefreshTokenNotFoundException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

@Service
class JWTService(
    @Value("\${jwt.secret:}") private val secretBase64: String,
    @Value("\${jwt.access-token-ttl:1}") private val accessTokenTtlHours: Long,
    @Value("\${jwt.refresh-token-ttl:168}") private val refreshTokenTtlHours: Long,
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val REFRESH_TOKEN_PREFIX = "refresh_token:"

    private val secretKey: SecretKey by lazy {
        if (secretBase64.isBlank()) {
            // Dev-only
            val devKey = "dev-secret-key-dev-secret-key-123456"
            Keys.hmacShaKeyFor(devKey.toByteArray(Charsets.UTF_8))
        } else {
            val keyBytes = Decoders.BASE64.decode(secretBase64)
            Keys.hmacShaKeyFor(keyBytes)
        }
    }

    // Access Token

    fun generateAccessToken(subject: String): String {
        val now = Date()
        val expiry = Date(now.time + TimeUnit.HOURS.toMillis(accessTokenTtlHours))
        return Jwts.builder()
            .claims()
            .subject(subject)
            .issuedAt(now)
            .expiration(expiry)
            .and()
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun extractUsername(token: String): String? =
        try {
            extractAllClaims(token)?.subject
        } catch (ex: JWTExpiredException) {
            throw JWTExpiredException("Token expired")
        } catch (ex: JwtException) {
            throw JWTMalformedException("Invalid token format: ${ex.message}")
        } catch (ex: Exception) {
            throw JWTValidationException("Token validation failed: ${ex.message}")
        }

    fun validateToken(token: String, username: String): Boolean {
        return try {
            val extracted = extractUsername(token)
            val exp = extractExpiration(token)
            extracted == username && exp?.after(Date()) == true
        } catch (ex: JWTException) {
            false
        }
    }

    private fun extractExpiration(token: String): Date? =
        try {
            extractAllClaims(token)?.expiration
        } catch (ex: ExpiredJwtException) {
            throw JWTExpiredException("Token expired")
        } catch (ex: JwtException) {
            throw JWTMalformedException("Invalid token format")
        } catch (ex: Exception) {
            throw JWTValidationException("Token validation failed")
        }

    private fun extractAllClaims(token: String): Claims? =
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (ex: ExpiredJwtException) {
            throw JWTExpiredException("Token expired")
        } catch (ex: JwtException) {
            throw JWTMalformedException("Invalid token format: ${ex.message}")
        } catch (ex: Exception) {
            throw JWTValidationException("Token validation failed: ${ex.message}")
        }

    // Refresh Token

    fun generateRefreshToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun createRefreshToken(username: String): String {
        val refreshToken = generateRefreshToken()
        val key = REFRESH_TOKEN_PREFIX + refreshToken

        redisTemplate.opsForValue().set(
            key,
            username,
            refreshTokenTtlHours,
            TimeUnit.HOURS
        )

        return refreshToken
    }

    fun consumeRefreshToken(token: String): String? {
        val key = REFRESH_TOKEN_PREFIX + token
        val username = redisTemplate.opsForValue().get(key)
            ?: throw RefreshTokenNotFoundException("Refresh token not found or expired")

        redisTemplate.delete(key)
        return username
    }

    fun deleteRefreshToken(refreshToken: String) {
        val key = REFRESH_TOKEN_PREFIX + refreshToken
        redisTemplate.delete(key)
    }
}