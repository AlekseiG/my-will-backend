package org.mywill.server.service

import org.mywill.server.controller.dto.ChangePasswordRequest
import org.mywill.server.controller.dto.ProfileDto
import org.mywill.server.controller.dto.UpdateProfileRequest
import org.mywill.server.entity.User
import org.mywill.server.repository.TrustedPersonRepository
import org.mywill.server.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Сервис для управления профилем пользователя.
 */
@Service
class ProfileService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val trustedPersonRepository: TrustedPersonRepository
) {

    /**
     * Возвращает данные профиля пользователя.
     * @param email Email пользователя.
     * @return DTO с данными профиля.
     * @throws RuntimeException если пользователь не найден.
     */
    @Transactional(readOnly = true)
    fun getProfile(email: String): ProfileDto {
        val user = userRepository.findByEmail(email) ?: throw RuntimeException("User not found")
        return user.toDto()
    }

    /**
     * Обновляет поля профиля (URL аватарки, таймаут смерти).
     * @param email Email пользователя.
     * @param request Объект с новыми данными профиля.
     * @return Обновленный DTO профиля.
     * @throws RuntimeException если пользователь не найден.
     */
    @Transactional
    fun updateProfile(email: String, request: UpdateProfileRequest): ProfileDto {
        val user = userRepository.findByEmail(email) ?: throw RuntimeException("User not found")
        request.avatarUrl?.let { user.avatarUrl = it }
        request.deathTimeoutSeconds?.let { user.deathTimeoutSeconds = it }
        return userRepository.save(user).toDto()
    }

    /**
     * Меняет пароль пользователя. Проверяет корректность старого пароля.
     * @param email Email пользователя.
     * @param request Объект с старым и новым паролями.
     * @throws RuntimeException если пользователь не найден, пароль не установлен (например, OAuth2 вход)
     * или старый пароль неверный.
     */
    @Transactional
    fun changePassword(email: String, request: ChangePasswordRequest) {
        val user = userRepository.findByEmail(email) ?: throw RuntimeException("User not found")
        val encoded = user.password ?: throw RuntimeException("Password not set")
        if (!passwordEncoder.matches(request.oldPassword, encoded)) {
            throw RuntimeException("Invalid old password")
        }
        user.password = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)
    }

    /**
     * Удаляет аккаунт пользователя и связанные с ним данные.
     * @param email Email пользователя.
     * @throws RuntimeException если пользователь не найден.
     */
    @Transactional
    fun deleteAccount(email: String) {
        val user = userRepository.findByEmail(email) ?: throw RuntimeException("User not found")
        // Logic to delete account: remove trusted relationships, wills, etc.
        // Assuming cascade delete or manual cleanup
        userRepository.delete(user)
    }

    /**
     * Метод "Я жив". Сбрасывает статус подтвержденной смерти, обнуляет время подтверждения
     * и отменяет все отметки "смерть подтверждена" от доверенных лиц.
     * @param email Email владельца профиля.
     * @throws RuntimeException если пользователь не найден.
     */
    @Transactional
    fun cancelDeathConfirmation(email: String) {
        val user = userRepository.findByEmail(email) ?: throw RuntimeException("User not found")
        user.isDead = false
        user.deathConfirmedAt = null
        // Reset confirmations from trusted people
        val trustedPeople = trustedPersonRepository.findByOwner(user)
        trustedPeople.forEach { it.confirmedDeath = false }
        trustedPersonRepository.saveAll(trustedPeople)
        userRepository.save(user)
    }

    private fun User.toDto() = ProfileDto(
        email = email,
        avatarUrl = avatarUrl,
        deathTimeoutSeconds = deathTimeoutSeconds,
        isDead = isDead,
        deathConfirmedAt = deathConfirmedAt,
        isSubscribed = isSubscribed
    )
}
