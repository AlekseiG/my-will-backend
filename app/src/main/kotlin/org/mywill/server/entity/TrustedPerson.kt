package org.mywill.server.entity

import jakarta.persistence.*

@Entity
@Table(name = "trusted_people")
class TrustedPerson(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    @Column(nullable = false)
    val email: String,

    @Column(name = "confirmed_death", nullable = false)
    var confirmedDeath: Boolean = false
)
