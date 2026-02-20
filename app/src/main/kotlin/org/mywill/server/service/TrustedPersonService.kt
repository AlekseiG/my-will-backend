package org.mywill.server.service

import org.mywill.server.controller.dto.TrustedPersonDto
import org.mywill.server.entity.TrustedPerson
import org.mywill.server.repository.TrustedPersonRepository
import org.mywill.server.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock

/**
 * Сервис для управления списком доверенных лиц.
 * Доверенные лица могут подтверждать факт смерти пользователя для запуска процесса открытия завещаний.
 */
@Service
class TrustedPersonService(
    private val trustedPersonRepository: TrustedPersonRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {

    /**
     * Возвращает список доверенных лиц текущего пользователя.
     * @param ownerEmail Email владельца списка.
     * @return Список DTO доверенных лиц.
     * @throws RuntimeException если пользователь не найден.
     */
    @Transactional(readOnly = true)
    fun getMyTrustedPeople(ownerEmail: String): List<TrustedPersonDto> {
        val owner = userRepository.findByEmail(ownerEmail) ?: throw RuntimeException("User not found")
        return trustedPersonRepository.findByOwner(owner).map { it.toDto() }
    }

    /**
     * Добавляет новое доверенное лицо. Если человек с таким email еще не зарегистрирован в системе,
     * ему отправляется приглашение по почте.
     * @param ownerEmail Email пользователя, добавляющего доверенное лицо.
     * @param emailToAdd Email добавляемого доверенного лица.
     * @return DTO созданного или существующего доверенного лица.
     * @throws RuntimeException если пользователь пытается добавить сам себя или если владелец не найден.
     */
    @Transactional
    fun addTrustedPerson(ownerEmail: String, emailToAdd: String): TrustedPersonDto {
        val owner = userRepository.findByEmail(ownerEmail) ?: throw RuntimeException("User not found")
        if (owner.email == emailToAdd) throw RuntimeException("Cannot add yourself")

        val existing = trustedPersonRepository.findByOwnerAndEmail(owner, emailToAdd)
        if (existing != null) return existing.toDto()

        val trustedPerson = TrustedPerson(owner = owner, email = emailToAdd)
        val saved = trustedPersonRepository.save(trustedPerson)

        // Check if user exists
        val user = userRepository.findByEmail(emailToAdd)
        if (user == null) {
            emailService.sendSimpleMessage(
                emailToAdd,
                "Приглашение в MyWill",
                "Вы были добавлены как доверенное лицо пользователем $ownerEmail. " +
                        "Наш сервис позволяет хранить завещания и открывать к ним доступ в случае смерти. " +
                        "Пожалуйста, зарегистрируйтесь, чтобы иметь возможность подтвердить факт смерти и получить доступ к завещаниям."
            )
        }

        return saved.toDto()
    }

    /**
     * Удаляет доверенное лицо из списка пользователя.
     * @param ownerEmail Email владельца списка.
     * @param id Идентификатор записи в таблице доверенных лиц.
     * @throws RuntimeException если владелец не найден, запись не существует или не принадлежит пользователю.
     */
    @Transactional
    fun removeTrustedPerson(ownerEmail: String, id: Long) {
        val owner = userRepository.findByEmail(ownerEmail) ?: throw RuntimeException("User not found")
        val tp = trustedPersonRepository.findById(id).orElseThrow { RuntimeException("Trusted person not found") }
        if (tp.owner.id != owner.id) throw RuntimeException("Access denied")
        trustedPersonRepository.delete(tp)
    }

    /**
     * Подтверждает факт смерти пользователя от лица доверенного лица.
     * Если ВСЕ доверенные лица пользователя подтвердили его смерть, фиксируется время `deathConfirmedAt`.
     * @param trustedPersonEmail Email доверенного лица, выполняющего действие.
     * @param ownerEmail Email пользователя, чья смерть подтверждается.
     * @throws RuntimeException если владелец не найден или пользователь не является его доверенным лицом.
     */
    @Transactional
    fun confirmDeath(trustedPersonEmail: String, ownerEmail: String) {
        val owner = userRepository.findByEmail(ownerEmail) ?: throw RuntimeException("Owner not found")
        val tp = trustedPersonRepository.findByOwnerAndEmail(owner, trustedPersonEmail)
            ?: throw RuntimeException("You are not a trusted person for this user")

        tp.confirmedDeath = true
        trustedPersonRepository.save(tp)

        val allTrusted = trustedPersonRepository.findByOwner(owner)
        if (allTrusted.all { it.confirmedDeath }) {
            owner.deathConfirmedAt = Clock.System.now()
            userRepository.save(owner)
        }
    }

    /**
     * Возвращает список email-ов пользователей, для которых текущий пользователь является доверенным лицом.
     * @param myEmail Email текущего пользователя.
     * @return Список email-ов владельцев завещаний.
     */
    @Transactional(readOnly = true)
    fun getWhoseTrustedIAm(myEmail: String): List<String> {
        return trustedPersonRepository.findByEmail(myEmail).map { it.owner.email }
    }

    private fun TrustedPerson.toDto() = TrustedPersonDto(id, email, confirmedDeath)
}
