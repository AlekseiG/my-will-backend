package org.mywill.server.repository

import org.mywill.server.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?

    @Query("SELECT u FROM User u WHERE u.deathConfirmedAt IS NOT NULL AND u.isDead = false")
    fun findByDeathConfirmedAtIsNotNullAndIsDeadFalse(): List<User>
}
