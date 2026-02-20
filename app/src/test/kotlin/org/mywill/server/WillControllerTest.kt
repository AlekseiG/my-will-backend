package org.mywill.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mywill.server.controller.dto.*
import org.mywill.server.repository.UserRepository
import org.mywill.server.service.EmailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.mockito.Mockito.mock

// Removed unused import

@ActiveProfiles("test")
class WillControllerTest : BaseIntegrationTest() {

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

    @Test
    fun testWillLifecycle() {
        val email = "will@example.com"
        val password = "password123"
        
        // 1. Регистрация и верификация
        mvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AuthRequest(email, password))
        }
        val code = userRepository.findByEmail(email)!!.verificationCode!!
        mvc.post("/auth/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(VerifyRequest(email, code))
        }

        // 2. Логин и получение токена
        val loginResponse = mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AuthRequest(email, password))
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }.andReturn()
        
        val token = objectMapper.readTree(loginResponse.response.contentAsString).get("token").asText()

        // 3. Получение списка своих завещаний (пустой)
        mvc.get("/api/will") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }

        // 4. Создание первого завещания
        val title1 = "Завещание 1"
        val content1 = "Контент 1"
        val createResponse1 = mvc.post("/api/will") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateWillRequest(title1, content1))
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value(title1) }
        }.andReturn()
        val will1Id = objectMapper.readTree(createResponse1.response.contentAsString).get("id").asLong()

        // 5. Создание второго завещания
        val title2 = "Завещание 2"
        val content2 = "Контент 2"
        mvc.post("/api/will") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateWillRequest(title2, content2))
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value(title2) }
        }

        // 6. Получение списка своих завещаний (2 штуки)
        mvc.get("/api/will") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }

        // 7. Добавление доступа к первому завещанию
        val friendEmail = "friend@example.com"
        mvc.post("/api/will/$will1Id/access") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddAccessRequest(friendEmail))
        }.andExpect {
            status { isOk() }
            jsonPath("$.allowedEmails[0]") { value(friendEmail) }
        }
    }

    @Test
    fun testUnauthenticatedAccess() {
        mvc.get("/api/will").andExpect {
            status { isUnauthorized() }
        }
    }
}
