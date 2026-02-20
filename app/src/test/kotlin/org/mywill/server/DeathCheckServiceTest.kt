package org.mywill.server

import org.junit.jupiter.api.Test
import org.mywill.server.entity.User
import org.mywill.server.repository.UserRepository
import org.mywill.server.service.DeathCheckService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@ActiveProfiles("test")
class DeathCheckServiceTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var deathCheckService: DeathCheckService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun testCheckDeaths() {
        val email = "death@test.com"
        val user = User(
            email = email,
            verified = true,
            deathTimeoutSeconds = 10,
            deathConfirmedAt = Clock.System.now() - 11.seconds
        )
        userRepository.save(user)
        deathCheckService.checkDeaths()
        val updatedUser = userRepository.findByEmail(email)!!
        assertTrue(updatedUser.isDead)
    }

    @Test
    fun testCheckDeathsStillPending() {
        val email = "pending@test.com"
        val user = User(
            email = email,
            verified = true,
            deathTimeoutSeconds = 10000,
            deathConfirmedAt = Clock.System.now() - 10.seconds
        )
        userRepository.save(user)
        deathCheckService.checkDeaths()

        val updatedUser = userRepository.findByEmail(email)!!
        assertFalse(updatedUser.isDead)
    }
}
