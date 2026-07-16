package com.example.auratracker.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiService(
    // По умолчанию адрес нашего купленного сервера в Польше!
    // Для отладки на локальном ПК в эмуляторе можно менять на "http://10.0.2.2:8000"
    private val baseUrl: String = "http://45.194.66.113:8000",
    private val tokenProvider: () -> String?
) {
    private val jsonInstance = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonInstance)
        }
        
        defaultRequest {
            val token = tokenProvider()
            if (token != null) {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun authenticateWithGoogle(idToken: String): Result<TokenResponse> = runCatching {
        client.post("$baseUrl/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleAuthRequest(idToken))
        }.body()
    }

    suspend fun processLog(text: String): Result<LogEntryResponse> = runCatching {
        client.post("$baseUrl/api/v1/logs/process") {
            contentType(ContentType.Application.Json)
            setBody(LogProcessRequest(text))
        }.body()
    }

    suspend fun fetchAllLogs(): Result<List<LogEntryResponse>> = runCatching {
        client.get("$baseUrl/api/v1/logs").body()
    }

    suspend fun deleteLog(id: String): Result<Unit> = runCatching {
        client.delete("$baseUrl/api/v1/logs/$id")
        Unit
    }
}
