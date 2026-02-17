package org.mywill.client

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(private val baseUrl: String = "http://localhost:8080") {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getHello(): String {
        return try {
            val response: HttpResponse = client.get("$baseUrl/")
            response.bodyAsText()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun getAdminUi(): String {
        return try {
            val response: HttpResponse = client.get("$baseUrl/admin/ui")
            response.bodyAsText()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun register(authRequest: AuthRequest): AuthResponse {
        return try {
            val response: HttpResponse = client.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(authRequest)
            }
            Json.decodeFromString<AuthResponse>(response.bodyAsText())
        } catch (e: Exception) {
            AuthResponse(false, "Error: ${e.message}")
        }
    }

    suspend fun login(authRequest: AuthRequest): AuthResponse {
        return try {
            val response: HttpResponse = client.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(authRequest)
            }
            Json.decodeFromString<AuthResponse>(response.bodyAsText())
        } catch (e: Exception) {
            AuthResponse(false, "Error: ${e.message}")
        }
    }

    suspend fun verify(verifyRequest: VerifyRequest): AuthResponse {
        return try {
            val response: HttpResponse = client.post("$baseUrl/auth/verify") {
                contentType(ContentType.Application.Json)
                setBody(verifyRequest)
            }
            Json.decodeFromString<AuthResponse>(response.bodyAsText())
        } catch (e: Exception) {
            AuthResponse(false, "Error: ${e.message}")
        }
    }

    fun close() {
        client.close()
    }
}
