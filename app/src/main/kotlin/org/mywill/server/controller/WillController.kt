package org.mywill.server.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mywill.server.controller.dto.*
import org.mywill.server.service.WillService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
private val logger = KotlinLogging.logger {}

/**
 * Контроллер для управления завещаниями.
 * Обеспечивает создание, просмотр, обновление и управление доступом.
 */
@RestController
@RequestMapping("/api/will")
class WillController(private val willService: WillService) {

    /**
     * Получает список всех завещаний текущего авторизованного пользователя.
     */
    @GetMapping
    fun getMyWills(principal: Principal): List<WillDto> {
        return willService.getMyWills(principal.name)
    }

    /**
     * Получает список завещаний других пользователей, к которым текущему предоставлен доступ.
     */
    @GetMapping("/shared")
    fun getSharedWills(principal: Principal): List<WillDto> {
        return willService.getSharedWills(principal.name)
    }

    /**
     * Возвращает конкретное завещание по ID, если у пользователя есть к нему доступ.
     */
    @GetMapping("/{id}")
    fun getWill(@PathVariable id: Long, principal: Principal): ResponseEntity<WillDto> {
        val will = willService.getWill(id, principal.name)
        return if (will != null) ResponseEntity.ok(will) else ResponseEntity.notFound().build()
    }

    /**
     * Создает новое завещание для текущего пользователя.
     */
    @PostMapping
    fun createWill(
        principal: Principal,
        @RequestBody request: CreateWillRequest
    ): WillDto {
        return willService.createWill(principal.name, request.title, request.content)
    }

    /**
     * Обновляет заголовок и содержание существующего завещания.
     */
    @PutMapping("/{id}")
    fun updateWill(
        @PathVariable id: Long,
        principal: Principal,
        @RequestBody request: UpdateWillRequest
    ): WillDto {
        return willService.updateWill(id, principal.name, request.title, request.content)
    }

    /**
     * Предоставляет доступ к завещанию другому пользователю по его email.
     */
    @PostMapping("/{id}/access")
    fun addAccess(
        @PathVariable id: Long,
        principal: Principal,
        @RequestBody request: AddAccessRequest
    ): WillDto {
        return willService.addAllowedEmail(id, principal.name, request.email)
    }
}
