package org.mywill.client

import kotlinx.serialization.Serializable

/**
 * Запрос для авторизации или регистрации пользователя.
 */
@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

/**
 * Ответ от сервера на попытку авторизации/регистрации.
 * @param success True, если операция прошла успешно.
 * @param message Сообщение от сервера (ошибка или статус).
 * @param token JWT токен (присутствует при успешном входе).
 */
@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null
)

/**
 * Запрос для верификации email.
 */
@Serializable
data class VerifyRequest(
    val email: String,
    val code: String
)

/**
 * Объект передачи данных (DTO) для завещания.
 */
@Serializable
data class WillDto(
    val id: Long? = null,
    val title: String = "",
    val content: String = "",
    val ownerEmail: String = "",
    val allowedEmails: Set<String> = emptySet()
)

/**
 * Запрос на создание нового завещания.
 */
@Serializable
data class CreateWillRequest(
    val title: String,
    val content: String
)

/**
 * Запрос на обновление существующего завещания.
 */
@Serializable
data class UpdateWillRequest(
    val title: String,
    val content: String
)

/**
 * Запрос на предоставление доступа к завещанию.
 */
@Serializable
data class AddAccessRequest(
    val email: String
)
