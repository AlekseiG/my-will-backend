package org.mywill.server.service

import org.mywill.server.entity.Will
import org.mywill.server.repository.WillRepository
import org.mywill.server.repository.UserRepository
import org.mywill.server.controller.dto.WillDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Сервис для работы с завещаниями.
 */
@Service
class WillService(
    private val willRepository: WillRepository,
    private val userRepository: UserRepository
) {

    /**
     * Возвращает список всех завещаний, принадлежащих пользователю с указанным email.
     * @param userEmail Email владельца завещаний.
     * @return Список DTO завещаний.
     */
    @Transactional(readOnly = true)
    fun getMyWills(userEmail: String): List<WillDto> {
        val user = userRepository.findByEmail(userEmail) ?: return emptyList()
        return willRepository.findByOwner(user).map { it.toDto() }
    }

    /**
     * Возвращает список завещаний других пользователей, к которым текущему пользователю предоставлен доступ.
     * Завещания отображаются только если их владелец официально признан умершим.
     * @param userEmail Email пользователя, запрашивающего доступные ему завещания.
     * @return Список DTO доступных чужих завещаний.
     */
    @Transactional(readOnly = true)
    fun getSharedWills(userEmail: String): List<WillDto> {
        return willRepository.findAllByAllowedEmail(userEmail)
            .filter { it.owner.isDead }
            .map { it.toDto() }
    }

    /**
     * Возвращает конкретное завещание по его идентификатору.
     * Проверяет права доступа: просматривать может либо владелец, либо человек из списка доступа
     * (при условии, что владелец умер).
     * @param id Идентификатор завещания.
     * @param userEmail Email пользователя, запрашивающего завещание.
     * @return DTO завещания или null, если оно не найдено.
     * @throws RuntimeException если доступ запрещен или завещание еще не открыто (владелец жив).
     */
    @Transactional(readOnly = true)
    fun getWill(id: Long, userEmail: String): WillDto? {
        val will = willRepository.findById(id).orElse(null) ?: return null
        val owner = will.owner
        if (owner.email != userEmail && !will.allowedEmails.contains(userEmail)) {
            throw RuntimeException("Access denied")
        }
        if (owner.email != userEmail && !owner.isDead) {
            throw RuntimeException("Will is not yet opened")
        }
        return will.toDto()
    }

    /**
     * Создает новое завещание.
     * @param userEmail Email создателя завещания.
     * @param title Заголовок завещания.
     * @param content Содержимое завещания.
     * @return DTO созданного завещания.
     * @throws RuntimeException если пользователь не найден.
     */
    @Transactional
    fun createWill(userEmail: String, title: String, content: String): WillDto {
        val user = userRepository.findByEmail(userEmail) ?: throw RuntimeException("User not found")
        val will = Will(owner = user, title = title, content = content)
        val savedWill = willRepository.save(will)
        return savedWill.toDto()
    }

    /**
     * Обновляет существующее завещание. Только владелец может редактировать свое завещание.
     * @param id Идентификатор завещания.
     * @param userEmail Email пользователя, пытающегося обновить завещание.
     * @param title Новый заголовок.
     * @param content Новое содержимое.
     * @return DTO обновленного завещания.
     * @throws RuntimeException если завещание не найдено или пользователь не является владельцем.
     */
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

    /**
     * Добавляет email пользователя в список тех, кто может прочитать данное завещание после смерти владельца.
     * Только владелец может изменять список доступа.
     * @param id Идентификатор завещания.
     * @param userEmail Email владельца завещания.
     * @param emailToAdd Email человека, которому предоставляется доступ.
     * @return DTO обновленного завещания.
     * @throws RuntimeException если завещание не найдено или пользователь не является владельцем.
     */
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
