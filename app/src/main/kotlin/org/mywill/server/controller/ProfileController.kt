package org.mywill.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.mywill.server.controller.dto.ChangePasswordRequest
import org.mywill.server.controller.dto.ProfileDto
import org.mywill.server.controller.dto.UpdateProfileRequest
import org.mywill.server.service.ProfileService
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Управление профилем пользователя")
class ProfileController(private val profileService: ProfileService) {

    @GetMapping
    @Operation(summary = "Получить данные профиля")
    fun getProfile(principal: Principal): ProfileDto {
        return profileService.getProfile(principal.name)
    }

    @PatchMapping
    @Operation(summary = "Обновить данные профиля")
    fun updateProfile(principal: Principal, @RequestBody request: UpdateProfileRequest): ProfileDto {
        return profileService.updateProfile(principal.name, request)
    }

    @PostMapping("/change-password")
    @Operation(summary = "Сменить пароль")
    fun changePassword(principal: Principal, @RequestBody request: ChangePasswordRequest) {
        profileService.changePassword(principal.name, request)
    }

    @DeleteMapping
    @Operation(summary = "Удалить аккаунт")
    fun deleteAccount(principal: Principal) {
        profileService.deleteAccount(principal.name)
    }

    @PostMapping("/cancel-death")
    @Operation(summary = "Отменить подтверждение смерти (Я жив)")
    fun cancelDeath(principal: Principal) {
        profileService.cancelDeathConfirmation(principal.name)
    }
}
