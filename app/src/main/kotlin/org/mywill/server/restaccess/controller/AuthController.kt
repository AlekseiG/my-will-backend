package org.mywill.server.restaccess.controller

import org.mywill.server.restaccess.dto.AuthRequest
import org.mywill.server.restaccess.dto.AuthResponse
import org.mywill.server.restaccess.dto.VerifyRequest
import org.mywill.server.service.AuthService
import org.springframework.web.bind.annotation.*

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
        println("[DEBUG_LOG] Login request received for email: ${request.email}")
        val response = authService.login(request)
        println("[DEBUG_LOG] Login response: $response")
        return response
    }
}
