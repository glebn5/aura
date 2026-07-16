package com.example.auratracker.data.remote

import com.example.auratracker.data.local.GeminiStructuredLog
import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequest(
    val id_token: String
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val telegram_id: String? = null,
    val created_at: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val user: UserResponse
)

@Serializable
data class LogProcessRequest(
    val raw_text: String
)

@Serializable
data class LogEntryResponse(
    val id: String,
    val user_id: String,
    val raw_text: String,
    val category: String,
    val structured_data: GeminiStructuredLog,
    val created_at: String
)
