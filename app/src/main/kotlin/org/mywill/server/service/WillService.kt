package org.mywill.server.service

import org.mywill.server.entity.Will
import org.mywill.server.repository.WillRepository
import org.mywill.server.repository.UserRepository
import org.mywill.server.controller.dto.WillDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WillService(
    private val willRepository: WillRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getMyWills(userEmail: String): List<WillDto> {
        val user = userRepository.findByEmail(userEmail) ?: return emptyList()
        return willRepository.findByOwner(user).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getSharedWills(userEmail: String): List<WillDto> {
        return willRepository.findAllByAllowedEmail(userEmail).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getWill(id: Long, userEmail: String): WillDto? {
        val will = willRepository.findById(id).orElse(null) ?: return null
        if (will.owner.email != userEmail && !will.allowedEmails.contains(userEmail)) {
            throw RuntimeException("Access denied")
        }
        return will.toDto()
    }

    @Transactional
    fun createWill(userEmail: String, title: String, content: String): WillDto {
        val user = userRepository.findByEmail(userEmail) ?: throw RuntimeException("User not found")
        val will = Will(owner = user, title = title, content = content)
        val savedWill = willRepository.save(will)
        return savedWill.toDto()
    }

    @Transactional
    fun updateWill(id: Long, userEmail: String, title: String, content: String): WillDto {
        val will = willRepository.findById(id).orElseThrow { RuntimeException("Will not found") }
        if (will.owner.email != userEmail) {
            throw RuntimeException("Only owner can update will")
        }
        will.title = title
        will.content = content
        return willRepository.save(will).toDto()
    }

    @Transactional
    fun addAllowedEmail(id: Long, userEmail: String, emailToAdd: String): WillDto {
        val will = willRepository.findById(id).orElseThrow { RuntimeException("Will not found") }
        if (will.owner.email != userEmail) {
            throw RuntimeException("Only owner can add access")
        }
        will.allowedEmails.add(emailToAdd)
        return willRepository.save(will).toDto()
    }

    private fun Will.toDto() = WillDto(id, title, content, owner.email, allowedEmails)
}
