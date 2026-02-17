package org.mywill.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mywill.server.controller.dto.AuthRequest
import org.mywill.server.controller.dto.VerifyRequest
import org.mywill.server.repository.UserRepository
import org.mywill.server.service.EmailService
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.mockito.Mockito.mock

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun emailService(): EmailService = mock(EmailService::class.java)
    }

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var emailService: EmailService

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        org.mockito.Mockito.reset(emailService)
    }

    @Test
    fun testRegisterVerifyAndLogin() {
        val email = "test@example.com"
        val password = "password123"
        val request = AuthRequest(email, password)

        // 1. Регистрация
        mvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        verify(emailService).sendVerificationCode(anyString(), anyString())

        // 2. Попытка логина без верификации (должна быть ошибка)
        mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.message") { value(org.hamcrest.Matchers.containsString("verify")) }
        }

        // Достаем код из базы для теста
        val user = userRepository.findByEmail(email)!!
        val code = user.verificationCode!!

        // 3. Верификация
        val verifyRequest = VerifyRequest(email, code)
        mvc.post("/auth/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        // 4. Логин после верификации
        mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    fun testLoginWithWrongPassword() {
        val registerRequest = AuthRequest("wrong@example.com", "password123")
        
        mvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(registerRequest)
        }.andExpect {
            status { isOk() }
        }

        val loginRequest = AuthRequest("wrong@example.com", "wrong_password")
        mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(false) }
        }
    }
}
