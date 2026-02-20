package org.mywill.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mywill.server.controller.dto.*
import org.mywill.server.repository.UserRepository
import org.mywill.server.service.EmailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put


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

    @Test
    fun testWillNegativeScenarios() {
        val ownerEmail = "owner-neg@example.com"
        val friendEmail = "friend-neg@example.com"
        val password = "password"

        // Setup owner
        mvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AuthRequest(ownerEmail, password))
        }
        val ownerCode = userRepository.findByEmail(ownerEmail)!!.verificationCode!!
        mvc.post("/auth/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(VerifyRequest(ownerEmail, ownerCode))
        }
        val ownerToken = objectMapper.readTree(
            mvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AuthRequest(ownerEmail, password))
            }.andReturn().response.contentAsString
        ).get("token").asText()

        // Setup friend
        mvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AuthRequest(friendEmail, password))
        }
        val friendCode = userRepository.findByEmail(friendEmail)!!.verificationCode!!
        mvc.post("/auth/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(VerifyRequest(friendEmail, friendCode))
        }
        val friendToken = objectMapper.readTree(
            mvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AuthRequest(friendEmail, password))
            }.andReturn().response.contentAsString
        ).get("token").asText()

        // 1. Create will
        val createRes = mvc.post("/api/will") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateWillRequest("Title", "Content"))
        }.andReturn()
        val willId = objectMapper.readTree(createRes.response.contentAsString).get("id").asLong()

        // 2. Friend tries to access will (Access Denied)
        mvc.get("/api/will/$willId") {
            header("Authorization", "Bearer $friendToken")
        }.andExpect {
            status { isForbidden() }
        }

        // 3. Add friend to access list
        mvc.post("/api/will/$willId/access") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddAccessRequest(friendEmail))
        }

        // 4. Friend tries to access will while owner is alive (Will not opened)
        mvc.get("/api/will/$willId") {
            header("Authorization", "Bearer $friendToken")
        }.andExpect {
            status { isForbidden() }
        }

        // 5. Owner is confirmed dead
        val ownerUser = userRepository.findByEmail(ownerEmail)!!
        ownerUser.isDead = true
        userRepository.save(ownerUser)

        // 6. Friend can now access will
        mvc.get("/api/will/$willId") {
            header("Authorization", "Bearer $friendToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content") { value("Content") }
        }

        // 7. Friend tries to update owner's will (Forbidden)
        mvc.put("/api/will/$willId") {
            header("Authorization", "Bearer $friendToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateWillRequest("New Title", "New Content"))
        }.andExpect {
            status { isForbidden() }
        }
    }
}
