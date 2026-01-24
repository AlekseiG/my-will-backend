package org.mywill.server.restaccess.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/ui")
class AdminUiController() {

    @GetMapping
    fun listRequests(
    ): String {
        return "hellowWorld"
    }


}
