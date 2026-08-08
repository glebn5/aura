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

import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class ApiService(
    private val baseUrlProvider: () -> String = { "http://10.0.2.2:8000" },
    private val tokenProvider: () -> String?
) {
    private val baseUrl: String get() = baseUrlProvider().trimEnd('/')
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
        val response = client.post("$baseUrl/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleAuthRequest(idToken))
        }
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
            error("Ошибка сервера ${response.status.value}: ${errorBody.ifBlank { response.status.description }}")
        }
        response.body()
    }

    suspend fun authenticateWithEmail(email: String): Result<TokenResponse> = runCatching {
        val response = client.post("$baseUrl/api/v1/auth/email") {
            contentType(ContentType.Application.Json)
            setBody(EmailAuthRequest(email))
        }
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
            error("Ошибка сервера ${response.status.value}: ${errorBody.ifBlank { response.status.description }}")
        }
        response.body()
    }

    suspend fun processLog(text: String): Result<LogEntryResponse> = runCatching {
        val response = client.post("$baseUrl/api/v1/logs/process") {
            contentType(ContentType.Application.Json)
            setBody(LogProcessRequest(text))
        }
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
            error("Ошибка сервера ${response.status.value}: ${errorBody.ifBlank { response.status.description }}")
        }
        response.body()
    }

    suspend fun fetchAllLogs(): Result<List<LogEntryResponse>> = runCatching {
        val response = client.get("$baseUrl/api/v1/logs")
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
            error("Ошибка сервера ${response.status.value}: ${errorBody.ifBlank { response.status.description }}")
        }
        response.body()
    }

    suspend fun deleteLog(id: String): Result<Unit> = runCatching {
        val response = client.delete("$baseUrl/api/v1/logs/$id")
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
            error("Ошибка сервера ${response.status.value}: ${errorBody.ifBlank { response.status.description }}")
        }
        Unit
    }
}

@kotlinx.serialization.Serializable
data class EmailAuthRequest(val email: String)
