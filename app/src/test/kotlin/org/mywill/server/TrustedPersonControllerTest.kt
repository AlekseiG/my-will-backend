package org.mywill.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mywill.server.controller.dto.AddTrustedPersonRequest
import org.mywill.server.controller.dto.DeathConfirmationRequest
import org.mywill.server.entity.User
import org.mywill.server.repository.TrustedPersonRepository
import org.mywill.server.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
@Transactional
class TrustedPersonControllerTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var trustedPersonRepository: TrustedPersonRepository

    private val ownerEmail = "owner@test.com"
    private val trustedEmail = "trusted@test.com"

    @BeforeEach
    fun setup() {
        trustedPersonRepository.deleteAll()
        userRepository.deleteAll()
        userRepository.save(User(email = ownerEmail, verified = true))
        userRepository.save(User(email = trustedEmail, verified = true))
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    fun testAddAndGetTrustedPeople() {
        val request = AddTrustedPersonRequest(email = trustedEmail)

        mvc.post("/api/trusted-people") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value(trustedEmail) }
        }

        mvc.get("/api/trusted-people")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].email") { value(trustedEmail) }
            }
    }

    @Test
    @WithMockUser(username = "trusted@test.com")
    fun testConfirmDeath() {
        // 1. Owner adds trusted person
        val owner = userRepository.findByEmail(ownerEmail)!!
        val tp = org.mywill.server.entity.TrustedPerson(owner = owner, email = trustedEmail)
        trustedPersonRepository.save(tp)

        // 2. Trusted person confirms death
        val request = DeathConfirmationRequest(ownerEmail = ownerEmail)
        mvc.post("/api/trusted-people/confirm-death") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isOk() }
        }

        val updatedOwner = userRepository.findByEmail(ownerEmail)!!
        assertNotNull(updatedOwner.deathConfirmedAt)

        val updatedTp = trustedPersonRepository.findByOwnerAndEmail(owner, trustedEmail)!!
        assertTrue(updatedTp.confirmedDeath)
    }

    @Test
    @WithMockUser(username = "trusted@test.com")
    fun testWhoseTrustedIAm() {
        val owner = userRepository.findByEmail(ownerEmail)!!
        trustedPersonRepository.save(org.mywill.server.entity.TrustedPerson(owner = owner, email = trustedEmail))

        mvc.get("/api/trusted-people/whose-trusted-i-am")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0]") { value(ownerEmail) }
            }
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    fun testAddSelfAsTrustedPersonFails() {
        val request = AddTrustedPersonRequest(email = "owner@test.com")
        mvc.post("/api/trusted-people") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    fun testRemoveTrustedPerson() {
        val owner = userRepository.findByEmail("owner@test.com")!!
        val tp =
            trustedPersonRepository.save(org.mywill.server.entity.TrustedPerson(owner = owner, email = trustedEmail))

        mvc.delete("/api/trusted-people/${tp.id}").andExpect {
            status { isOk() }
        }

        assertTrue(trustedPersonRepository.findById(tp.id!!).isEmpty)
    }

    @Test
    @WithMockUser(username = "someone@else.com")
    fun testRemoveOthersTrustedPersonFails() {
        val owner = userRepository.findByEmail(ownerEmail)!!
        val tp =
            trustedPersonRepository.save(org.mywill.server.entity.TrustedPerson(owner = owner, email = trustedEmail))

        userRepository.save(User(email = "someone@else.com", verified = true))

        mvc.delete("/api/trusted-people/${tp.id}").andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    fun testRemoveNonExistentTrustedPersonFails() {
        mvc.delete("/api/trusted-people/99999").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @WithMockUser(username = "not-trusted@test.com")
    fun testConfirmDeathNotTrustedPerson() {
        val request = DeathConfirmationRequest(ownerEmail = ownerEmail)
        mvc.post("/api/trusted-people/confirm-death") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }
}
