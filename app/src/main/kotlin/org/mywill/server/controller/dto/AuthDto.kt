package org.mywill.server.controller.dto

/**
 * Запрос для авторизации или регистрации пользователя.
 */
data class AuthRequest(
    val email: String = "",
    val password: String = ""
)

/**
 * Ответ от сервера на попытку авторизации/регистрации.
 */
data class AuthResponse(
    val success: Boolean = false,
    val message: String = "",
    val token: String? = null
)

/**
 * Запрос для верификации email.
 */
data class VerifyRequest(
    val email: String = "",
    val code: String = ""
)
