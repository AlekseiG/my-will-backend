package org.mywill.server.entity

import jakarta.persistence.*

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
    var verificationCode: String? = null
)
