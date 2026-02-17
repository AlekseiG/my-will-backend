package org.mywill.server.restaccess.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mywill.server.restaccess.dto.AuthRequest
import org.mywill.server.restaccess.dto.AuthResponse
import org.mywill.server.restaccess.dto.VerifyRequest
import org.mywill.server.service.AuthService
import org.springframework.web.bind.annotation.*
private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody request: AuthRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/verify")
    fun verify(@RequestBody request: VerifyRequest): AuthResponse {
        return authService.verify(request)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): AuthResponse {
        logger.debug {"Login request received for email: ${request.email}"}
        val response = authService.login(request)
        logger.debug {"Login response: $response"}
        return response
    }
}
