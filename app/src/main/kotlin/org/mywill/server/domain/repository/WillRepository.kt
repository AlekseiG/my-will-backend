package org.mywill.server.domain.repository

import org.mywill.server.domain.entity.Will
import org.mywill.server.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WillRepository : JpaRepository<Will, Long> {
    fun findByOwner(owner: User): Will?
}
