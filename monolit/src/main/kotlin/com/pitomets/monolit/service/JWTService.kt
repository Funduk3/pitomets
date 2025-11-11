package com.pitomets.monolit.service

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Claims
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
        } catch (ex: Exception) {
            null
        }

    fun validateToken(token: String, username: String): Boolean {
        val extracted = extractUsername(token) ?: return false
        if (extracted != username) return false
        val exp = extractExpiration(token) ?: return false
        return !exp.before(Date())
    }

    private fun extractExpiration(token: String): Date? =
        try {
            extractAllClaims(token)?.expiration
        } catch (ex: Exception) {
            null
        }

    private fun extractAllClaims(token: String): Claims? =
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (ex: JwtException) {
            null
        } catch (ex: Exception) {
            null
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

    fun consumeRefreshToken(refreshToken: String): String? {
        val key = REFRESH_TOKEN_PREFIX + refreshToken
        val username = redisTemplate.opsForValue().get(key) ?: return null
        redisTemplate.delete(key)
        return username
    }

    fun deleteRefreshToken(refreshToken: String) {
        val key = REFRESH_TOKEN_PREFIX + refreshToken
        redisTemplate.delete(key)
    }

    fun deleteAllUserRefreshTokens(username: String) {
        val pattern = "$REFRESH_TOKEN_PREFIX*"
        val keys = redisTemplate.keys(pattern)

        keys.forEach { key ->
            val storedUsername = redisTemplate.opsForValue().get(key)
            if (storedUsername == username) {
                redisTemplate.delete(key)
            }
        }
    }
}