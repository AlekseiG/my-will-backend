package org.mywill.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mywill.server.controller.dto.ChangePasswordRequest
import org.mywill.server.controller.dto.UpdateProfileRequest
import org.mywill.server.entity.User
import org.mywill.server.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.delete
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val testEmail = "profile@test.com"

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()
        userRepository.save(User(
            email = testEmail,
            password = passwordEncoder.encode("oldPassword"),
            verified = true
        ))
    }

    @Test
    @WithMockUser(username = "profile@test.com")
    fun testGetProfile() {
        mvc.get("/api/profile")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                jsonPath("$.email") { value(testEmail) }
            }
    }

    @Test
    @WithMockUser(username = "profile@test.com")
    fun testUpdateProfile() {
        val request = UpdateProfileRequest(
            avatarUrl = "http://avatar.com/1.png",
            deathTimeoutSeconds = 3600
        )

        mvc.patch("/api/profile") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.avatarUrl") { value("http://avatar.com/1.png") }
            jsonPath("$.deathTimeoutSeconds") { value(3600) }
        }

        val user = userRepository.findByEmail(testEmail)!!
        assertEquals("http://avatar.com/1.png", user.avatarUrl)
        assertEquals(3600, user.deathTimeoutSeconds)
    }

    @Test
    @WithMockUser(username = "profile@test.com")
    fun testChangePassword() {
        val request = ChangePasswordRequest(
            oldPassword = "oldPassword",
            newPassword = "newPassword"
        )

        mvc.post("/api/profile/change-password") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }

        val user = userRepository.findByEmail(testEmail)!!
        assertTrue(passwordEncoder.matches("newPassword", user.password))
    }

    @Test
    @WithMockUser(username = "profile@test.com")
    fun testCancelDeath() {
        val user = userRepository.findByEmail(testEmail)!!
        user.isDead = true
        user.deathConfirmedAt = Clock.System.now()
        userRepository.save(user)

        mvc.post("/api/profile/cancel-death") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        val updatedUser = userRepository.findByEmail(testEmail)!!
        assertFalse(updatedUser.isDead)
        assertNull(updatedUser.deathConfirmedAt)
    }

    @Test
    @WithMockUser(username = "profile@test.com")
    fun testDeleteAccount() {
        mvc.delete("/api/profile")
            .andExpect {
                status { isOk() }
            }

        assertNull(userRepository.findByEmail(testEmail))
    }
}
