package com.pitomets.monolit.service

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Claims
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

// непонятно где лучше хранить секретный ключ
@Component
class JWTService(
    @Value("\${jwt.secret:}") private val secretBase64: String
) {

    private val secretKey: SecretKey by lazy {
        if (secretBase64.isBlank()) {
            Jwts.SIG.HS256.key().build()
        } else {
            val keyBytes = Decoders.BASE64.decode(secretBase64)
            Keys.hmacShaKeyFor(keyBytes)
        }
    }

    fun generateToken(subject: String, ttlHours: Long = 30): String {
        val now = Date()
        val expiry = Date(now.time + TimeUnit.HOURS.toMillis(ttlHours))
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
}
