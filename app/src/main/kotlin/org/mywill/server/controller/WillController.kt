package org.mywill.server.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.mywill.server.controller.dto.AddAccessRequest
import org.mywill.server.controller.dto.WillDto
import org.mywill.server.service.WillService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Создает новое завещание для текущего пользователя с вложениями.")
    fun createWill(
        @RequestPart("title") title: String,
        @RequestPart("content") content: String,
        @RequestPart("attachments", required = false) attachments: List<String>?,
        @RequestPart("files", required = false) files: List<MultipartFile>?,
        principal: Principal
    ): WillDto {
        val fileMap = files?.associate { it.originalFilename!! to it.bytes } ?: emptyMap()
        return willService.createWill(principal.name, title, content, attachments ?: emptyList(), fileMap)
    }

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Обновляет существующее завещание с вложениями.")
    fun updateWill(
        @PathVariable id: Long,
        @RequestPart("title") title: String,
        @RequestPart("content") content: String,
        @RequestPart("attachments", required = false) attachments: List<String>?,
        @RequestPart("files", required = false) files: List<MultipartFile>?,
        principal: Principal
    ): WillDto {
        val fileMap = files?.associate { it.originalFilename!! to it.bytes } ?: emptyMap()
        return willService.updateWill(id, principal.name, title, content, attachments ?: emptyList(), fileMap)
    }

    @GetMapping("/{id}/attachment/{key}")
    @Operation(summary = "Загружает вложение по ключу.")
    fun downloadAttachment(
        @PathVariable id: Long,
        @PathVariable key: String,
        principal: Principal
    ): ResponseEntity<ByteArray> {
        val data = willService.downloadAttachment(id, key, principal.name)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$key\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data)
    }

    @PostMapping("/{id}/access")
    @Operation(summary = "Предоставляет доступ к завещанию другому пользователю по его email.")
    fun addAccess(
        @PathVariable id: Long,
        @RequestBody request: AddAccessRequest,
        principal: Principal
    ): WillDto {
        return willService.addAllowedEmail(id, principal.name, request.email)
    }
}
