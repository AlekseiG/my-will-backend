package org.mywill.server.restaccess.controller

import org.springframework.web.bind.annotation.*

@RestController
class AdminUiController() {

    @GetMapping("/")
    fun hello(): String {
        return "Greetings from Spring Boot!"
    }
}
