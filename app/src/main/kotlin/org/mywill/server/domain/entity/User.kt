package org.mywill.server.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var verified: Boolean = false,

    @Column(name = "verification_code")
    var verificationCode: String? = null
)
