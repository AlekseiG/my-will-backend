package org.mywill.server.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.mywill.server.controller.dto.AuthRequest
import org.mywill.server.controller.dto.AuthResponse
import org.mywill.server.controller.dto.VerifyRequest
import org.mywill.server.service.AuthService
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Управление аутентификацией и регистрацией")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя")
    fun register(@RequestBody request: AuthRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/verify")
    @Operation(summary = "Подтверждение регистрации с помощью кода, отправленного на email.")
    fun verify(@RequestBody request: VerifyRequest): AuthResponse {
        return authService.verify(request)
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Возвращает JWT токен при успешном входе")
    fun login(@RequestBody request: AuthRequest): AuthResponse {
        logger.debug {"Login request received for email: ${request.email}"}
        val response = authService.login(request)
        logger.debug {"Login response: $response"}
        return response
    }
}
