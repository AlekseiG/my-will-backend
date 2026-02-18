package org.mywill.server.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mywill.server.controller.dto.AuthRequest
import org.mywill.server.controller.dto.AuthResponse
import org.mywill.server.controller.dto.VerifyRequest
import org.mywill.server.service.AuthService
import org.springframework.web.bind.annotation.*
private val logger = KotlinLogging.logger {}

/**
 * Контроллер для аутентификации пользователей.
 * Обрабатывает регистрацию, подтверждение email и вход в систему.
 */
@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    /**
     * Регистрация нового пользователя.
     */
    @PostMapping("/register")
    fun register(@RequestBody request: AuthRequest): AuthResponse {
        return authService.register(request)
    }

    /**
     * Подтверждение регистрации с помощью кода, отправленного на email.
     */
    @PostMapping("/verify")
    fun verify(@RequestBody request: VerifyRequest): AuthResponse {
        return authService.verify(request)
    }

    /**
     * Вход в систему. Возвращает JWT токен при успешной авторизации.
     */
    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): AuthResponse {
        logger.debug {"Login request received for email: ${request.email}"}
        val response = authService.login(request)
        logger.debug {"Login response: $response"}
        return response
    }
}
