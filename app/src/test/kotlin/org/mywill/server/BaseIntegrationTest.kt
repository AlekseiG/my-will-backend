package org.mywill.server

import org.junit.jupiter.api.BeforeEach
import org.mywill.server.repository.UserRepository
import org.mywill.server.repository.WillRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var willRepository: WillRepository

    companion object {
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:15-alpine").apply {
            start()
        }
    }

    @BeforeEach
    fun clearDatabase() {
        willRepository.deleteAll()
        userRepository.deleteAll()
    }
}
