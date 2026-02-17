package org.mywill.server.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mywill.server.controller.dto.*
import org.mywill.server.service.WillService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/will")
class WillController(private val willService: WillService) {

    @GetMapping
    fun getMyWills(principal: Principal): List<WillDto> {
        return willService.getMyWills(principal.name)
    }

    @GetMapping("/shared")
    fun getSharedWills(principal: Principal): List<WillDto> {
        return willService.getSharedWills(principal.name)
    }

    @GetMapping("/{id}")
    fun getWill(@PathVariable id: Long, principal: Principal): ResponseEntity<WillDto> {
        val will = willService.getWill(id, principal.name)
        return if (will != null) ResponseEntity.ok(will) else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun createWill(
        principal: Principal,
        @RequestBody request: CreateWillRequest
    ): WillDto {
        return willService.createWill(principal.name, request.title, request.content)
    }

    @PutMapping("/{id}")
    fun updateWill(
        @PathVariable id: Long,
        principal: Principal,
        @RequestBody request: UpdateWillRequest
    ): WillDto {
        return willService.updateWill(id, principal.name, request.title, request.content)
    }

    @PostMapping("/{id}/access")
    fun addAccess(
        @PathVariable id: Long,
        principal: Principal,
        @RequestBody request: AddAccessRequest
    ): WillDto {
        return willService.addAllowedEmail(id, principal.name, request.email)
    }
}
