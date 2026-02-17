package org.mywill.client

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(private val baseUrl: String = "http://localhost:8080") {
    private var authToken: String? = null

    private val client = try {
        println("[DEBUG_LOG] HttpClient initializing...")
        val c = HttpClient {
            install(ContentNegotiation) {
                println("[DEBUG_LOG] ContentNegotiation installing...")
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(HttpCookies)
        }
        println("[DEBUG_LOG] HttpClient initialized successfully")
        c
    } catch (e: Exception) {
        println("[ERROR_LOG] HttpClient initialization failed: ${e.message}")
        throw e
    }

    private fun HttpRequestBuilder.withCredentials() {
        header(HttpHeaders.AccessControlAllowCredentials, "true")
        authToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    suspend fun getHello(): String {
        return try {
            val response: HttpResponse = client.get("$baseUrl/") {
                withCredentials()
            }
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
                withCredentials()
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
                withCredentials()
            }
            val result = Json.decodeFromString<AuthResponse>(response.bodyAsText())
            if (result.success && result.token != null) {
                authToken = result.token
                println("[DEBUG_LOG] Auth token saved")
            }
            result
        } catch (e: Exception) {
            AuthResponse(false, "Error: ${e.message}")
        }
    }

    suspend fun verify(verifyRequest: VerifyRequest): AuthResponse {
        return try {
            val response: HttpResponse = client.post("$baseUrl/auth/verify") {
                contentType(ContentType.Application.Json)
                setBody(verifyRequest)
                withCredentials()
            }
            Json.decodeFromString<AuthResponse>(response.bodyAsText())
        } catch (e: Exception) {
            AuthResponse(false, "Error: ${e.message}")
        }
    }

    suspend fun getWill(): WillDto? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/will") {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                Json.decodeFromString<WillDto>(response.bodyAsText())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateWill(request: UpdateWillRequest): WillDto? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/will") {
                contentType(ContentType.Application.Json)
                setBody(request)
                withCredentials()
            }
            Json.decodeFromString<WillDto>(response.bodyAsText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addAccess(request: AddAccessRequest): WillDto? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/will/access") {
                contentType(ContentType.Application.Json)
                setBody(request)
                withCredentials()
            }
            Json.decodeFromString<WillDto>(response.bodyAsText())
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        client.close()
    }
}
