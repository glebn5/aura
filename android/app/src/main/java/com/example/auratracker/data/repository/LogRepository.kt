package com.example.auratracker.data.repository

import android.content.Context
import com.example.auratracker.data.local.GeminiStructuredLog
import com.example.auratracker.data.local.LogDao
import com.example.auratracker.data.local.LogEntryEntity
import com.example.auratracker.data.remote.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class LogRepository(
    private val context: Context,
    private val logDao: LogDao
) {
    private val sharedPrefs = context.getSharedPreferences("auratracker_prefs", Context.MODE_PRIVATE)
    
    // Получение токена из SharedPreferences для Ktor клиента
    fun getAuthToken(): String? {
        return sharedPrefs.getString("jwt_token", null)
    }

    private val apiService = ApiService { getAuthToken() }
    
    val logsFlow: Flow<List<LogEntryEntity>> = logDao.getAllLogsFlow()

    /**
     * Авторизация через Google ID Token. Сохраняет JWT и обновляет историю.
     */
    suspend fun authenticateWithGoogle(idToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        apiService.authenticateWithGoogle(idToken).map { response ->
            sharedPrefs.edit()
                .putString("jwt_token", response.access_token)
                .putString("user_email", response.user.email)
                .apply()
            
            // Сразу после логина скачиваем историю
            refreshHistory()
        }
    }

    fun logout() {
        sharedPrefs.edit().clear().apply()
        CoroutineScope(Dispatchers.IO).launch {
            logDao.clearAllLogs()
        }
    }

    fun isUserLoggedIn(): Boolean {
        return getAuthToken() != null
    }

    fun getUserEmail(): String {
        return sharedPrefs.getString("user_email", "guest@auratracker.ru") ?: "guest@auratracker.ru"
    }

    /**
     * Создание новой текстовой записи лога (оффлайн-сэйв + синхронизация).
     */
    suspend fun sendLog(text: String) = withContext(Dispatchers.IO) {
        val entryId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        // Создаем пустой структурированный лог по умолчанию (категория OTHER)
        val defaultStructured = GeminiStructuredLog(category = "OTHER", other_summary = text)
        val defaultJson = Json.encodeToString(defaultStructured)

        // 1. Быстро сохраняем локально со статусом PENDING
        val localLog = LogEntryEntity(
            id = entryId,
            rawText = text,
            category = "OTHER",
            structuredDataJson = defaultJson,
            createdAt = timestamp,
            syncStatus = "PENDING"
        )
        logDao.insertLog(localLog)

        // 2. Пробуем отправить на сервер
        trySyncEntry(entryId, text)
    }

    /**
     * Попытка синхронизации конкретной записи.
     */
    private suspend fun trySyncEntry(id: String, text: String) {
        apiService.processLog(text).fold(
            onSuccess = { response ->
                val json = Json.encodeToString(response.structured_data)
                logDao.updateSyncSuccess(
                    id = id,
                    status = "SYNCED",
                    category = response.category,
                    json = json
                )
            },
            onFailure = { error ->
                // Оставляем PENDING, фоновый планировщик синхронизирует позже
                error.printStackTrace()
            }
        )
    }

    /**
     * Полная принудительная синхронизация всех оффлайн-записей.
     */
    suspend fun syncPendingLogs() = withContext(Dispatchers.IO) {
        val pending = logDao.getPendingLogs()
        for (log in pending) {
            trySyncEntry(log.id, log.rawText)
        }
    }

    /**
     * Загрузка истории логов из сервера и обновление локального Room кэша.
     */
    suspend fun refreshHistory() = withContext(Dispatchers.IO) {
        apiService.fetchAllLogs().fold(
            onSuccess = { remoteLogs ->
                val entities = remoteLogs.map { remote ->
                    LogEntryEntity(
                        id = remote.id,
                        rawText = remote.raw_text,
                        category = remote.category,
                        structuredDataJson = Json.encodeToString(remote.structured_data),
                        createdAt = parseIsoDateTime(remote.created_at),
                        syncStatus = "SYNCED"
                    )
                }
                logDao.clearAllLogs()
                logDao.insertAll(entities)
            },
            onFailure = {
                // Если нет интернета, просто остаемся на локальном кэше Room
                it.printStackTrace()
            }
        )
    }

    /**
     * Удаление записи.
     */
    suspend fun deleteLog(id: String) = withContext(Dispatchers.IO) {
        // Удаляем локально сразу
        logDao.deleteLogById(id)
        // И шлем запрос на сервер (fire-and-forget)
        apiService.deleteLog(id)
    }

    /**
     * Вспомогательный метод парсинга ISO даты в timestamp.
     */
    private fun parseIsoDateTime(isoString: String): Long {
        return try {
            // Убираем возможную "Z" в конце и миллисекунды для упрощения парсинга, если потребуется
            val cleanStr = isoString.replace("Z", "+00:00")
            // Современные версии Android поддерживают java.time
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.OffsetDateTime.parse(cleanStr).toInstant().toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
