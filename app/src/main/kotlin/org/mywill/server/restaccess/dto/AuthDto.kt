package org.mywill.server.restaccess.dto

data class AuthRequest(
    val email: String = "",
    val password: String = ""
)

data class AuthResponse(
    val success: Boolean = false,
    val message: String = ""
)

data class VerifyRequest(
    val email: String = "",
    val code: String = ""
)
