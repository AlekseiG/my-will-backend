package org.mywill.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mywill.server.restaccess.dto.AuthRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun testRegisterAndLogin() {
        val request = AuthRequest("test@example.com", "password123")

        // 1. Регистрация
        mvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        // 2. Повторная регистрация (должна быть ошибка или успех в зависимости от логики, обычно ошибка)
        // Но пока проверим только успешный логин
        
        // 3. Логин
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
