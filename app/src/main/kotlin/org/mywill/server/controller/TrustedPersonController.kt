package org.mywill.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.mywill.server.controller.dto.AddTrustedPersonRequest
import org.mywill.server.controller.dto.DeathConfirmationRequest
import org.mywill.server.controller.dto.TrustedPersonDto
import org.mywill.server.service.TrustedPersonService
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/trusted-people")
@Tag(name = "Trusted People", description = "Управление доверенными лицами")
class TrustedPersonController(private val trustedPersonService: TrustedPersonService) {

    @GetMapping
    @Operation(summary = "Получить мой список доверенных лиц")
    fun getMyTrustedPeople(principal: Principal): List<TrustedPersonDto> {
        return trustedPersonService.getMyTrustedPeople(principal.name)
    }

    @PostMapping
    @Operation(summary = "Добавить доверенное лицо")
    fun addTrustedPerson(principal: Principal, @RequestBody request: AddTrustedPersonRequest): TrustedPersonDto {
        return trustedPersonService.addTrustedPerson(principal.name, request.email)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить доверенное лицо")
    fun removeTrustedPerson(principal: Principal, @PathVariable id: Long) {
        trustedPersonService.removeTrustedPerson(principal.name, id)
    }

    @PostMapping("/confirm-death")
    @Operation(summary = "Подтвердить смерть пользователя", description = "Используется доверенным лицом")
    fun confirmDeath(principal: Principal, @RequestBody request: DeathConfirmationRequest) {
        trustedPersonService.confirmDeath(principal.name, request.ownerEmail)
    }

    @GetMapping("/whose-trusted-i-am")
    @Operation(summary = "Список пользователей, для которых я доверенное лицо")
    fun getWhoseTrustedIAm(principal: Principal): List<String> {
        return trustedPersonService.getWhoseTrustedIAm(principal.name)
    }
}
