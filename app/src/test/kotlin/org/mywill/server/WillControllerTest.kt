package org.mywill.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mywill.server.restaccess.dto.AddAccessRequest
import org.mywill.server.restaccess.dto.AuthRequest
import org.mywill.server.restaccess.dto.UpdateWillRequest
import org.mywill.server.restaccess.dto.VerifyRequest
import org.mywill.server.domain.repository.UserRepository
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
import org.springframework.mock.web.MockHttpSession
// Removed unused import

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WillControllerTest {

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

        // 2. Логин и получение сессии
        val loginResponse = mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AuthRequest(email, password))
        }.andDo { print() }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }.andReturn()
        
        val token = objectMapper.readTree(loginResponse.response.contentAsString).get("token").asText()

        // 3. Получение завещания (должно быть 404, так как еще не создано)
        mvc.get("/api/will") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNotFound() }
        }

        // 4. Создание/обновление завещания
        val willContent = "Это мое завещание.\n1. Кота оставить соседу.\n2. Книги в библиотеку."
        mvc.post("/api/will") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateWillRequest(willContent))
        }.andExpect {
            status { isOk() }
            jsonPath("$.content") { value(willContent) }
        }

        // 5. Получение завещания снова
        mvc.get("/api/will") {
            header("Authorization", "Bearer $token")
        }.andDo { print() }.andExpect {
            status { isOk() }
            jsonPath("$.content") { value(willContent) }
        }

        // 6. Добавление доступа
        val accessEmail = "friend@example.com"
        mvc.post("/api/will/access") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddAccessRequest(accessEmail))
        }.andExpect {
            status { isOk() }
            jsonPath("$.allowedEmails[0]") { value(accessEmail) }
        }
    }

    @Test
    fun testUnauthenticatedAccess() {
        mvc.get("/api/will").andExpect {
            status { isForbidden() }
        }
    }
}
