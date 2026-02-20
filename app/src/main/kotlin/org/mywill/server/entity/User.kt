package org.mywill.server.entity

import jakarta.persistence.*
import kotlin.time.Instant

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = true)
    var password: String? = null,

    @Column(nullable = false)
    var verified: Boolean = false,

    @Column(name = "verification_code")
    var verificationCode: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column(name = "death_timeout_seconds", nullable = false)
    var deathTimeoutSeconds: Long = 86400,

    @Column(name = "is_dead", nullable = false)
    var isDead: Boolean = false,

    @Column(name = "death_confirmed_at")
    var deathConfirmedAt: Instant? = null
)
