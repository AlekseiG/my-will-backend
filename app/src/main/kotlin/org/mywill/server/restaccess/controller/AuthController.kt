package org.mywill.server.restaccess.controller

import org.mywill.server.restaccess.dto.AuthRequest
import org.mywill.server.restaccess.dto.AuthResponse
import org.mywill.server.service.AuthService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody request: AuthRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): AuthResponse {
        return authService.login(request)
    }
}
