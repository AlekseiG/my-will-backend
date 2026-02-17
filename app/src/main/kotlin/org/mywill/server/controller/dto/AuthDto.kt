package org.mywill.server.controller.dto

data class AuthRequest(
    val email: String = "",
    val password: String = ""
)

data class AuthResponse(
    val success: Boolean = false,
    val message: String = "",
    val token: String? = null
)

data class VerifyRequest(
    val email: String = "",
    val code: String = ""
)
