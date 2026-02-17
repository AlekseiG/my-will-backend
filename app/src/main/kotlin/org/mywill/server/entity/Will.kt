package org.mywill.server.entity

import jakarta.persistence.*

@Entity
@Table(name = "wills")
class Will(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "will_access_emails", joinColumns = [JoinColumn(name = "will_id")])
    @Column(name = "email")
    var allowedEmails: MutableSet<String> = mutableSetOf()
)
