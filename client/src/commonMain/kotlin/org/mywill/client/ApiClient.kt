package org.mywill.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class ApiClient(private val baseUrl: String = "http://localhost:8080") {
    private val client = HttpClient()

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

    fun close() {
        client.close()
    }
}
