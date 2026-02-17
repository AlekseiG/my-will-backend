package org.mywill.server.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtils {
    // В реальном приложении секрет должен быть в конфиге
    private val secret = "my-very-secret-key-that-is-at-least-32-characters-long"
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())
    private val expirationMs = 86400000 // 24 часа

    fun generateToken(email: String): String {
        return Jwts.builder()
            .subject(email)
            .issuedAt(Date())
            .expiration(Date(Date().time + expirationMs))
            .signWith(key)
            .compact()
    }

    fun getEmailFromToken(token: String): String? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
        } catch (e: Exception) {
            null
        }
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
