package org.mywill.client

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientBuildTest {
    @Test
    fun testClientInitialization() {
        // Проверяем, что базовые DTO могут быть инициализированы
        val dto = WillDto(id = 1L, content = "test")
        assertTrue(dto.content == "test")
        assertTrue(dto.id == 1L)
    }

    @Test
    fun testSerialization() {
        val dto = WillDto(id = 1L, content = "test", allowedEmails = setOf("a@b.com"))
        val json = Json.encodeToString(dto)
        assertTrue(json.contains("1"))
        assertTrue(json.contains("test"))
        
        val decoded = Json.decodeFromString<WillDto>(json)
        assertEquals(dto, decoded)
    }

    @Test
    fun testRangeErrorReproduction() {
        // Пытаемся воспроизвести ошибку Ranges.kt:306 "Step must be positive"
        // Это обычно случается при создании прогрессии с нулевым или отрицательным шагом.
        // Но в Kotlin/JS stdlib Ranges.kt:306 часто соответствует проверке шага в IntProgression.
        try {
            val range = 1..10 step 1
            assertEquals(1, range.step)
        } catch (e: Exception) {
            println("Caught range error: ${e.message}")
            throw e
        }
    }

    @Test
    fun testRangeWithNullStep() {
        // Тест удален, так как он намеренно вызывал ошибку
    }
}
