package org.mywill.server
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableScheduling
class BotApplication {

    @Value("\${app.frontend-base-url:http://localhost:8081}")
    private lateinit var frontendBaseUrl: String

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedOrigins(frontendBaseUrl)
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowCredentials(true)
            }
        }
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(e: RuntimeException): ResponseEntity<Map<String, String>> {
        val status = when (e.message) {
            "Access denied", "Only owner can update will", "Only owner can add access", "Will is not yet opened", "You are not a trusted person for this user" -> HttpStatus.FORBIDDEN
            "Cannot add yourself" -> HttpStatus.BAD_REQUEST
            "User not found", "Will not found", "Owner not found", "Trusted person not found" -> HttpStatus.NOT_FOUND
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        return ResponseEntity.status(status).body(mapOf("error" to (e.message ?: "Internal Server Error")))
    }
}

fun main(args: Array<String>) {
    runApplication<BotApplication>(*args)
}
