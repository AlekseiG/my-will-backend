package org.mywill.client

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null
)

@Serializable
data class VerifyRequest(
    val email: String,
    val code: String
)

@Serializable
data class WillDto(
    val id: Long? = null,
    val content: String = "",
    val allowedEmails: Set<String> = emptySet()
)

@Serializable
data class UpdateWillRequest(
    val content: String
)

@Serializable
data class AddAccessRequest(
    val email: String
)
