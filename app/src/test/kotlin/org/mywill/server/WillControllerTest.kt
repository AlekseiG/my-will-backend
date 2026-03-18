package org.mywill.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mywill.server.controller.dto.AddAccessRequest
import org.mywill.server.controller.dto.AttachmentDto
import org.mywill.server.controller.dto.AuthRequest
import org.mywill.server.controller.dto.VerifyRequest
import org.mywill.server.service.EmailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post


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
        val createResponse1 = mvc.multipart("/api/will") {
            header("Authorization", "Bearer $token")
            file("title", title1.toByteArray())
            file("content", content1.toByteArray())
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value(title1) }
        }.andReturn()
        val will1Id = objectMapper.readTree(createResponse1.response.contentAsString).get("id").asLong()

        // 5. Создание второго завещания
        val title2 = "Завещание 2"
        val content2 = "Контент 2"
        mvc.multipart("/api/will") {
            header("Authorization", "Bearer $token")
            file("title", title2.toByteArray())
            file("content", content2.toByteArray())
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
        val createRes = mvc.multipart("/api/will") {
            header("Authorization", "Bearer $ownerToken")
            file("title", "Title".toByteArray())
            file("content", "Content".toByteArray())
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
        mvc.multipart("/api/will/$willId") {
            header("Authorization", "Bearer $friendToken")
            with {
                it.method = "PUT"
                it
            }
            file("title", "New Title".toByteArray())
            file("content", "New Content".toByteArray())
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun testWillAttachmentsAndSubscription() {
        val email = "subscriber@example.com"
        val password = "password"

        // Register and login
        mvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AuthRequest(email, password))
        }
        val code = userRepository.findByEmail(email)!!.verificationCode!!
        mvc.post("/auth/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(VerifyRequest(email, code))
        }
        val token = objectMapper.readTree(
            mvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AuthRequest(email, password))
            }.andReturn().response.contentAsString
        ).get("token").asText()

        // 1. Try to create will with attachments without subscription (Should fail)
        mvc.multipart("/api/will") {
            header("Authorization", "Bearer $token")
            file("title", "Title".toByteArray())
            file("content", "Content".toByteArray())
            file(
                "attachments",
                objectMapper.writeValueAsBytes(listOf(AttachmentDto("key1", "file.mp4")))
            )
        }.andExpect {
            status { isForbidden() }
        }

        // 2. Set user as subscribed
        val user = userRepository.findByEmail(email)!!
        user.isSubscribed = true
        userRepository.save(user)

        // 3. Create will with attachments with subscription (Should succeed)
        val attachments = listOf(
            AttachmentDto("key1", "video.mp4"),
            AttachmentDto("key2", "photo.jpg")
        )
        val createRes = mvc.multipart("/api/will") {
            header("Authorization", "Bearer $token")
            file("title", "Subscribed Title".toByteArray())
            file("content", "Subscribed Content".toByteArray())
            file("attachments", objectMapper.writeValueAsBytes(attachments))
        }.andExpect {
            status { isOk() }
            jsonPath("$.attachments.length()") { value(2) }
            jsonPath("$.attachments[0].key") { value(attachments[0].key) }
            jsonPath("$.attachments[0].name") { value(attachments[0].name) }
            jsonPath("$.attachments[1].key") { value(attachments[1].key) }
            jsonPath("$.attachments[1].name") { value(attachments[1].name) }
        }.andReturn()

        val willId = objectMapper.readTree(createRes.response.contentAsString).get("id").asLong()

        // 4. Update will with new attachments
        val newAttachments = listOf(AttachmentDto("key3", "audio.mp3"))
        mvc.multipart("/api/will/$willId") {
            header("Authorization", "Bearer $token")
            with {
                it.method = "PUT"
                it
            }
            file("title", "Updated Title".toByteArray())
            file("content", "Updated Content".toByteArray())
            file("attachments", objectMapper.writeValueAsBytes(newAttachments))
        }.andExpect {
            status { isOk() }
            jsonPath("$.attachments.length()") { value(1) }
            jsonPath("$.attachments[0].key") { value(newAttachments[0].key) }
            jsonPath("$.attachments[0].name") { value(newAttachments[0].name) }
        }
    }
}
