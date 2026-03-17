package org.mywill.server.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект передачи данных для вложения.
 */
@Schema(description = "Данные вложения")
data class AttachmentDto(
    @Schema(description = "Ключ в S3")
    val key: String,
    @Schema(description = "Оригинальное имя файла")
    val name: String
)

/**
 * Объект передачи данных для завещания.
 */
@Schema(description = "Данные завещания")
data class WillDto(
    @Schema(description = "ID завещания")
    val id: Long? = null,
    @Schema(description = "Заголовок")
    val title: String = "",
    @Schema(description = "Содержимое")
    val content: String = "",
    @Schema(description = "Email владельца")
    val ownerEmail: String = "",
    @Schema(description = "Список email, которым разрешен доступ")
    val allowedEmails: Set<String> = emptySet(),
    @Schema(description = "Список вложений")
    val attachments: List<AttachmentDto> = emptyList()
)

/**
 * Запрос на создание завещания.
 */
@Schema(description = "Запрос на создание завещания")
data class CreateWillRequest(
    @Schema(description = "Заголовок")
    val title: String,
    @Schema(description = "Содержимое")
    val content: String,
    @Schema(description = "Список вложений")
    val attachments: List<AttachmentDto> = emptyList()
)

/**
 * Запрос на обновление завещания.
 */
@Schema(description = "Запрос на обновление завещания")
data class UpdateWillRequest(
    @Schema(description = "Заголовок")
    val title: String,
    @Schema(description = "Содержимое")
    val content: String,
    @Schema(description = "Список вложений")
    val attachments: List<AttachmentDto> = emptyList()
)

/**
 * Запрос на добавление доступа к завещанию.
 */
@Schema(description = "Запрос на добавление доступа")
data class AddAccessRequest(
    @Schema(description = "Email пользователя")
    val email: String
)
