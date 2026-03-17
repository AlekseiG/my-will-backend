package org.mywill.client

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.mywill.client.ui.SelectedFile

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
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(HttpCookies)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
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
            json.decodeFromString<AuthResponse>(response.bodyAsText())
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
            val result = json.decodeFromString<AuthResponse>(response.bodyAsText())
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
            json.decodeFromString<AuthResponse>(response.bodyAsText())
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
                json.decodeFromString<List<WillDto>>(response.bodyAsText())
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
                json.decodeFromString<List<WillDto>>(response.bodyAsText())
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
                json.decodeFromString<WillDto>(response.bodyAsText())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Создание нового завещания с вложениями (Multipart).
     */
    suspend fun createWill(
        title: String,
        content: String,
        attachments: List<AttachmentDto>,
        files: List<SelectedFile>
    ): WillDto? {
        return try {
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "$baseUrl/api/will",
                formData = formData {
                    append("title", title)
                    append("content", content)
                    if (attachments.isNotEmpty()) {
                        append("attachments", json.encodeToString(attachments))
                    }
                    files.forEach { file ->
                        append("files", file.bytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }
                }
            ) {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<WillDto>(response.bodyAsText())
            } else null
        } catch (e: Exception) {
            println("[ERROR_LOG] Create will failed: ${e.message}")
            null
        }
    }

    /**
     * Обновление завещания с вложениями (Multipart).
     */
    suspend fun updateWill(
        id: Long,
        title: String,
        content: String,
        attachments: List<AttachmentDto>,
        files: List<SelectedFile>
    ): WillDto? {
        return try {
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "$baseUrl/api/will/$id",
                formData = formData {
                    append("title", title)
                    append("content", content)
                    if (attachments.isNotEmpty()) {
                        append("attachments", json.encodeToString(attachments))
                    }
                    files.forEach { file ->
                        append("files", file.bytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }
                }
            ) {
                method = HttpMethod.Put
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<WillDto>(response.bodyAsText())
            } else null
        } catch (e: Exception) {
            println("[ERROR_LOG] Update will failed: ${e.message}")
            null
        }
    }

    fun getDownloadUrl(willId: Long, key: String): String {
        return "$baseUrl/api/will/$willId/attachment/$key"
    }

    /**
     * Загружает файл по указанному URL.
     */
    suspend fun downloadFile(url: String): ByteArray? {
        return try {
            val response: HttpResponse = client.get(url) {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                response.readBytes()
            } else null
        } catch (e: Exception) {
            println("[ERROR_LOG] Download failed: ${e.message}")
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
            json.decodeFromString<WillDto>(response.bodyAsText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProfile(): ProfileDto? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/profile") {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                val text = response.bodyAsText()
                println("[DEBUG_LOG] Profile response: $text")
                try {
                    json.decodeFromString<ProfileDto>(text)
                } catch (e: Exception) {
                    println("[ERROR_LOG] Profile decoding error: ${e.message}")
                    null
                }
            } else null
        } catch (e: Exception) {
            println("[ERROR_LOG] getProfile request failed: ${e.message}")
            null
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): ProfileDto? {
        return try {
            val response: HttpResponse = client.patch("$baseUrl/api/profile") {
                contentType(ContentType.Application.Json)
                setBody(request)
                withCredentials()
            }
            json.decodeFromString<ProfileDto>(response.bodyAsText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun changePassword(request: ChangePasswordRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/profile/change-password") {
                contentType(ContentType.Application.Json)
                setBody(request)
                withCredentials()
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteAccount(): Boolean {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/profile") {
                withCredentials()
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun cancelDeath(): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/profile/cancel-death") {
                withCredentials()
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMyTrustedPeople(): List<TrustedPersonDto> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/trusted-people") {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<List<TrustedPersonDto>>(response.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addTrustedPerson(request: AddTrustedPersonRequest): TrustedPersonDto? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/trusted-people") {
                contentType(ContentType.Application.Json)
                setBody(request)
                withCredentials()
            }
            json.decodeFromString<TrustedPersonDto>(response.bodyAsText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun removeTrustedPerson(id: Long): Boolean {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/trusted-people/$id") {
                withCredentials()
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun confirmDeath(request: DeathConfirmationRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/trusted-people/confirm-death") {
                contentType(ContentType.Application.Json)
                setBody(request)
                withCredentials()
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWhoseTrustedIAm(): List<String> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/trusted-people/whose-trusted-i-am") {
                withCredentials()
            }
            if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<List<String>>(response.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Закрытие клиента и освобождение ресурсов.
     */
    fun close() {
        client.close()
    }
}
