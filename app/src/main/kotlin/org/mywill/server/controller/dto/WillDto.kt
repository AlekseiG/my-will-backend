package org.mywill.server.controller.dto

/**
 * Объект передачи данных для завещания.
 */
data class WillDto(
    val id: Long? = null,
    val title: String = "",
    val content: String = "",
    val ownerEmail: String = "",
    val allowedEmails: Set<String> = emptySet()
)

/**
 * Запрос на создание завещания.
 */
data class CreateWillRequest(
    val title: String,
    val content: String
)

/**
 * Запрос на обновление завещания.
 */
data class UpdateWillRequest(
    val title: String,
    val content: String
)

/**
 * Запрос на добавление доступа к завещанию.
 */
data class AddAccessRequest(
    val email: String
)
