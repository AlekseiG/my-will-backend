package org.mywill.server.repository

import org.mywill.server.entity.TrustedPerson
import org.mywill.server.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TrustedPersonRepository : JpaRepository<TrustedPerson, Long> {
    fun findByOwner(owner: User): List<TrustedPerson>
    fun findByEmail(email: String): List<TrustedPerson>
    fun findByOwnerAndEmail(owner: User, email: String): TrustedPerson?
}
