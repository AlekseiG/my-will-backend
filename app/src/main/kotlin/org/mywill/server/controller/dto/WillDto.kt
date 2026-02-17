package org.mywill.server.controller.dto

data class WillDto(
    val id: Long? = null,
    val title: String = "",
    val content: String = "",
    val ownerEmail: String = "",
    val allowedEmails: Set<String> = emptySet()
)

data class CreateWillRequest(
    val title: String,
    val content: String
)

data class UpdateWillRequest(
    val title: String,
    val content: String
)

data class AddAccessRequest(
    val email: String
)
