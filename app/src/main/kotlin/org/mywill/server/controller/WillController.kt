package org.mywill.server.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.mywill.server.controller.dto.AddAccessRequest
import org.mywill.server.controller.dto.CreateWillRequest
import org.mywill.server.controller.dto.UpdateWillRequest
import org.mywill.server.controller.dto.WillDto
import org.mywill.server.service.WillService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/will")
@Tag(name = "Will", description = "Управление завещаниями")
class WillController(private val willService: WillService) {

    @GetMapping
    @Operation(summary = "Получает список всех завещаний текущего авторизованного пользователя.")
    fun getMyWills(principal: Principal): List<WillDto> {
        return willService.getMyWills(principal.name)
    }

    @GetMapping("/shared")
    @Operation(summary = "Получает список завещаний других пользователей, к которым текущему предоставлен доступ.")
    fun getSharedWills(principal: Principal): List<WillDto> {
        return willService.getSharedWills(principal.name)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Возвращает конкретное завещание по ID, если у пользователя есть к нему доступ.")
    fun getWill(@PathVariable id: Long, principal: Principal): ResponseEntity<WillDto> {
        val will = willService.getWill(id, principal.name)
        return if (will != null) ResponseEntity.ok(will) else ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Создает новое завещание для текущего пользователя.")
    fun createWill(
        principal: Principal,
        @RequestBody request: CreateWillRequest
    ): WillDto {
        return willService.createWill(principal.name, request.title, request.content, request.attachments)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновляет заголовок и содержание существующего завещания.")
    fun updateWill(
        @PathVariable id: Long,
        principal: Principal,
        @RequestBody request: UpdateWillRequest
    ): WillDto {
        return willService.updateWill(id, principal.name, request.title, request.content, request.attachments)
    }

    @PostMapping("/{id}/access")
    @Operation(summary = "Предоставляет доступ к завещанию другому пользователю по его email.")
    fun addAccess(
        @PathVariable id: Long,
        principal: Principal,
        @RequestBody request: AddAccessRequest
    ): WillDto {
        return willService.addAllowedEmail(id, principal.name, request.email)
    }
}
