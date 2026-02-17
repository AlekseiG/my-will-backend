package org.mywill.server.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mywill.server.controller.dto.AddAccessRequest
import org.mywill.server.controller.dto.UpdateWillRequest
import org.mywill.server.controller.dto.WillDto
import org.mywill.server.service.WillService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/will")
class WillController(private val willService: WillService) {

    @GetMapping
    fun getWill(principal: Principal): ResponseEntity<WillDto> {
        logger.debug { "[DEBUG_LOG] Get will request for user: ${principal.name}"}
        val will = willService.getWill(principal.name)
        return if (will != null) ResponseEntity.ok(will) else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun updateWill(
        principal: Principal,
        @RequestBody request: UpdateWillRequest
    ): WillDto {
        logger.debug {"[DEBUG_LOG] Update will request received for user: ${principal.name}"}
        val response = willService.updateWillContent(principal.name, request.content)
        logger.debug {"[DEBUG_LOG] Update will response: $response"}
        return response
    }

    @PostMapping("/access")
    fun addAccess(
        principal: Principal,
        @RequestBody request: AddAccessRequest
    ): WillDto {
        return willService.addAllowedEmail(principal.name, request.email)
    }
}
