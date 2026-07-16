package com.example.auratracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json

@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey
    val id: String, // UUID в виде строки
    val rawText: String,
    val category: String,
    val structuredDataJson: String, // Храним как JSON-строку
    val createdAt: Long,
    val syncStatus: String // "PENDING" или "SYNCED"
) {
    // Вспомогательный метод для получения структурированного лога
    fun getStructuredLog(): GeminiStructuredLog? {
        return try {
            Json.decodeFromString<GeminiStructuredLog>(structuredDataJson)
        } catch (e: Exception) {
            null
        }
    }
}
