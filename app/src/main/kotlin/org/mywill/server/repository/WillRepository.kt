package org.mywill.server.repository

import org.mywill.server.entity.Will
import org.mywill.server.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface WillRepository : JpaRepository<Will, Long> {
    fun findByOwner(owner: User): List<Will>
    
    @Query("SELECT w FROM Will w JOIN w.allowedEmails e WHERE e = :email")
    fun findAllByAllowedEmail(email: String): List<Will>
}
