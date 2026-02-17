package org.mywill.server.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "users", schema = "my_will")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    val password: String
)
