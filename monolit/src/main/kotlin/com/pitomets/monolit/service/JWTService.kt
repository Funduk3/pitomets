package com.pitomets.monolit.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.collections.HashMap


@Component
class JWTService {
    private var secretkey = ""

    init {
        try {
            val keyGen: KeyGenerator = KeyGenerator.getInstance("HmacSHA256")
            val sk: SecretKey = keyGen.generateKey()
            secretkey = Base64.getEncoder().encodeToString(sk.encoded)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    fun generateToken(username: String?): String {
        val claims: Map<String, Any> = HashMap()
        return Jwts.builder()
            .claims()
            .add(claims)
            .subject(username)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + 60 * 60 * 30))
            .and()
            .signWith(key)
            .compact()
    }

    private val key: Key
        get() {
            val keyBytes = Decoders.BASE64.decode(secretkey)
            return Keys.hmacShaKeyFor(keyBytes)
        }
}