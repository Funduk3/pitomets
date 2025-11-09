package com.pitomets.monolit.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

@Component
class JWTFilter(
    private val secretKeyBase64: String =
        Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().encoded)
) {
    fun generateToken(username: String): String {
        val claims: Map<String, Any> = emptyMap()
        val now = Date()
        val expiry = Date(now.time + TimeUnit.HOURS.toMillis(30))

        return Jwts.builder()
            .claims()
                .add(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
            .and()
            .signWith(key)
            .compact()
    }


    private val key: SecretKey
        get() {
            val keyBytes = Decoders.BASE64.decode(secretKeyBase64)
            return Keys.hmacShaKeyFor(keyBytes)
        }

    fun extractUserName(token: String): String =
        extractClaim(token) { it.subject ?: "" }

    private fun <T> extractClaim(token: String, claimResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimResolver(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val userName = try {
            extractUserName(token)
        } catch (ex: Exception) {
            return false
        }
        return (userName == userDetails.username && !isTokenExpired(token))
    }

    private fun isTokenExpired(token: String): Boolean {
        val expiration = try {
            extractExpiration(token)
        } catch (ex: Exception) {
            return true
        }
        return expiration.before(Date())
    }

    private fun extractExpiration(token: String): Date =
        extractClaim(token) { it.expiration }
}
