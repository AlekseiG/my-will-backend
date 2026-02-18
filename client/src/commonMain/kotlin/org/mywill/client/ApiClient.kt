package org.mywill.client

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Клиент для взаимодействия с API бэкенда.
 * Использует Ktor для выполнения HTTP-запросов и Kotlinx.serialization для JSON.
 * 
 * @param baseUrl Базовый URL API (по умолчанию http://localhost:8080).
 */
class ApiClient(private val baseUrl: String = "http://localhost:8080") {
    private var authToken: String? = null

    /**
     * Экземпляр HttpClient с настроенным ContentNegotiation (JSON) и поддержкой Cookies.
     */
    private val client = try {
        println("[DEBUG_LOG] HttpClient initializing...")
        HttpClient {
            install(ContentNegotiation) {
                println("[DEBUG_LOG] ContentNegotiation installing...")
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(HttpCookies)
        }
    } catch (e: Exception) {
        println("[ERROR_LOG] HttpClient initialization failed: ${e.message}")
        throw e
    }

    /**
     * Добавляет заголовок авторизации к запросу, если токен установлен.
     */
    private fun HttpRequestBuilder.withCredentials() {
        header(HttpHeaders.AccessControlAllowCredentials, "true")
        authToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    /**
     * Устанавливает токен авторизации вручную.
     * Используется для OAuth2 или восстановления сессии.
     */
    fun setToken(token: String) {
        authToken = token
        println("[DEBUG_LOG] Auth token manually set")
    }

    /**
     * Проверка связи с сервером.
     * @return Текст ответа или сообщение об ошибке.
     */
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

    /**
     * Регистрация нового пользователя.
     */
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

    /**
     * Авторизация пользователя. При успехе сохраняет полученный токен.
     */
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

    /**
     * Верификация аккаунта (подтверждение через код из email).
     */
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

    /**
     * Получение списка завещаний текущего пользователя.
     */
    suspend fun getMyWills(): List<WillDto> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/will") {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                Json.decodeFromString<List<WillDto>>(response.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Получение списка завещаний, к которым пользователю предоставлен доступ.
     */
    suspend fun getSharedWills(): List<WillDto> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/will/shared") {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                Json.decodeFromString<List<WillDto>>(response.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Получение конкретного завещания по ID.
     */
    suspend fun getWill(id: Long): WillDto? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/will/$id") {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                Json.decodeFromString<WillDto>(response.bodyAsText())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Создание нового завещания.
     */
    suspend fun createWill(request: CreateWillRequest): WillDto? {
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

    /**
     * Обновление существующего завещания.
     */
    suspend fun updateWill(id: Long, request: UpdateWillRequest): WillDto? {
        return try {
            val response: HttpResponse = client.put("$baseUrl/api/will/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
                withCredentials()
            }
            Json.decodeFromString<WillDto>(response.bodyAsText())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Предоставление доступа к завещанию другому пользователю по email.
     */
    suspend fun addAccess(id: Long, request: AddAccessRequest): WillDto? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/will/$id/access") {
                contentType(ContentType.Application.Json)
                setBody(request)
                withCredentials()
            }
            Json.decodeFromString<WillDto>(response.bodyAsText())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Закрытие клиента и освобождение ресурсов.
     */
    fun close() {
        client.close()
    }
}
