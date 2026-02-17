package org.mywill.server.repository

import org.mywill.server.entity.Will
import org.mywill.server.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WillRepository : JpaRepository<Will, Long> {
    fun findByOwner(owner: User): Will?
}
