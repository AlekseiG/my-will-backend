package org.mywill.server.restaccess.dto

data class WillDto(
    val id: Long? = null,
    val content: String = "",
    val allowedEmails: Set<String> = emptySet()
)

data class UpdateWillRequest(
    val content: String
)

data class AddAccessRequest(
    val email: String
)
