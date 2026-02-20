package org.mywill.server.controller.dto
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Запрос для авторизации или регистрации пользователя.
 */
@Schema(description = "Запрос для авторизации или регистрации")
data class AuthRequest(
    @Schema(description = "Email пользователя", example = "user@example.com")
    val email: String = "",
    @Schema(description = "Пароль пользователя", example = "password123")
    val password: String = ""
)

/**
 * Ответ от сервера на попытку авторизации/регистрации.
 */
@Schema(description = "Ответ на авторизацию")
data class AuthResponse(
    @Schema(description = "Флаг успешности операции")
    val success: Boolean = false,
    @Schema(description = "Сообщение от сервера")
    val message: String = "",
    @Schema(description = "JWT токен (при успешном входе)")
    val token: String? = null
)

/**
 * Запрос для верификации email.
 */
@Schema(description = "Запрос для верификации email")
data class VerifyRequest(
    @Schema(description = "Email пользователя", example = "user@example.com")
    val email: String = "",
    @Schema(description = "Код верификации", example = "1234")
    val code: String = ""
)
