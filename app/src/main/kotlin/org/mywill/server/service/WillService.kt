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
    fun getWill(userEmail: String): WillDto? {
        val user = userRepository.findByEmail(userEmail) ?: return null
        val will = willRepository.findByOwner(user) ?: return null
        return WillDto(will.id, will.content, will.allowedEmails)
    }

    @Transactional
    fun updateWillContent(userEmail: String, content: String): WillDto {
        val user = userRepository.findByEmail(userEmail) ?: throw RuntimeException("User not found")
        val will = willRepository.findByOwner(user) ?: Will(owner = user, content = content)
        will.content = content
        val savedWill = willRepository.save(will)
        return WillDto(savedWill.id, savedWill.content, savedWill.allowedEmails)
    }

    @Transactional
    fun addAllowedEmail(userEmail: String, emailToAdd: String): WillDto {
        val user = userRepository.findByEmail(userEmail) ?: throw RuntimeException("User not found")
        val will = willRepository.findByOwner(user) ?: throw RuntimeException("Will not found. Create it first.")
        will.allowedEmails.add(emailToAdd)
        val savedWill = willRepository.save(will)
        return WillDto(savedWill.id, savedWill.content, savedWill.allowedEmails)
    }
}
