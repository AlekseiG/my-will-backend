package org.mywill.server.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import kotlin.time.Instant

@Schema(description = "Профиль пользователя")
data class ProfileDto(
    @Schema(description = "Email пользователя")
    val email: String,
    @Schema(description = "URL аватарки")
    val avatarUrl: String?,
    @Schema(description = "Таймаут подтверждения смерти в секундах")
    val deathTimeoutSeconds: Long,
    @Schema(description = "Флаг, признан ли пользователь умершим")
    val isDead: Boolean,
    @Schema(description = "Дата и время подтверждения смерти")
    val deathConfirmedAt: Instant? = null,
    @Schema(description = "Наличие оплаченной подписки")
    val isSubscribed: Boolean = false
)

@Schema(description = "Запрос на обновление профиля")
data class UpdateProfileRequest(
    @Schema(description = "Новый URL аватарки")
    val avatarUrl: String? = null,
    @Schema(description = "Новый таймаут подтверждения смерти")
    val deathTimeoutSeconds: Long? = null
)

@Schema(description = "Запрос на смену пароля")
data class ChangePasswordRequest(
    @Schema(description = "Старый пароль")
    val oldPassword: String,
    @Schema(description = "Новый пароль")
    val newPassword: String
)

@Schema(description = "Данные доверенного лица")
data class TrustedPersonDto(
    @Schema(description = "ID записи")
    val id: Long?,
    @Schema(description = "Email доверенного лица")
    val email: String,
    @Schema(description = "Подтвердил ли этот человек смерть владельца")
    val confirmedDeath: Boolean
)

@Schema(description = "Запрос на добавление доверенного лица")
data class AddTrustedPersonRequest(
    @Schema(description = "Email человека")
    val email: String
)

@Schema(description = "Запрос на подтверждение смерти")
data class DeathConfirmationRequest(
    @Schema(description = "Email владельца, чью смерть нужно подтвердить")
    val ownerEmail: String
)
