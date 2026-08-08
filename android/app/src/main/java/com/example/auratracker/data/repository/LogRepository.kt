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

import com.example.auratracker.data.local.CustomDashboardDao
import com.example.auratracker.data.local.CustomDashboardEntity

class LogRepository(
    private val context: Context,
    private val logDao: LogDao,
    private val customDashboardDao: CustomDashboardDao
) {
    private val sharedPrefs = context.getSharedPreferences("auratracker_prefs", Context.MODE_PRIVATE)

    val dashboardsFlow: Flow<List<CustomDashboardEntity>> = customDashboardDao.getAllDashboardsFlow()

    suspend fun seedDefaultDashboardsIfEmpty() = withContext(Dispatchers.IO) {
        if (customDashboardDao.getDashboardsCount() == 0) {
            val defaultDashboards = listOf(
                CustomDashboardEntity(
                    id = "default_fitness",
                    title = "Fitness",
                    titleRu = "Фитнес",
                    promptQuery = "фитнес и тренировки",
                    categoryFilter = "FITNESS",
                    accentColorHex = "#AEEA00",
                    iconName = "fitness"
                ),
                CustomDashboardEntity(
                    id = "default_finance",
                    title = "Finance",
                    titleRu = "Финансы",
                    promptQuery = "расходы и покупки",
                    categoryFilter = "FINANCE",
                    accentColorHex = "#00E5FF",
                    iconName = "finance"
                ),
                CustomDashboardEntity(
                    id = "default_auto",
                    title = "Auto",
                    titleRu = "Авто",
                    promptQuery = "расходы на авто",
                    categoryFilter = "CAR_MAINTENANCE",
                    accentColorHex = "#FF9100",
                    iconName = "car"
                )
            )
            customDashboardDao.insertAll(defaultDashboards)
        }
    }

    suspend fun deleteDashboard(id: String) = withContext(Dispatchers.IO) {
        customDashboardDao.deleteDashboardById(id)
    }

    suspend fun addDashboardFromPrompt(prompt: String) = withContext(Dispatchers.IO) {
        val p = prompt.lowercase()
        val timeDays = when {
            p.contains("недел") || p.contains("week") -> 7
            p.contains("год") || p.contains("year") -> 365
            else -> 30
        }

        val (category, subCategory, color, icon, titleEn, titleRu) = when {
            p.contains("отжим") || p.contains("pushup") -> 
                Tuple6("FITNESS", "pushups", "#AEEA00", "fitness", "Pushups Stats", "Отжимания")
            p.contains("подтягив") || p.contains("pullup") -> 
                Tuple6("FITNESS", "pullups", "#AEEA00", "fitness", "Pullups Stats", "Подтягивания")
            p.contains("бег") || p.contains("run") -> 
                Tuple6("FITNESS", "running", "#AEEA00", "fitness", "Running Distance", "Пробежки")
            p.contains("бензин") || p.contains("топлив") || p.contains("gas") -> 
                Tuple6("CAR_MAINTENANCE", "gas", "#FF9100", "car", "Fuel Expenses", "Расходы на бензин")
            p.contains("продукт") || p.contains("магазин") || p.contains("groc") -> 
                Tuple6("FINANCE", "groceries", "#00E5FF", "finance", "Groceries Stats", "Траты на продукты")
            p.contains("кофе") || p.contains("coffee") -> 
                Tuple6("FINANCE", "coffee", "#E040FB", "finance", "Coffee Expenses", "Расходы на кофе")
            p.contains("финанс") || p.contains("расход") || p.contains("трат") || p.contains("spend") -> 
                Tuple6("FINANCE", null, "#00E5FF", "finance", if (timeDays == 7) "Weekly Expenses" else "Monthly Expenses", if (timeDays == 7) "Расходы за неделю" else "Траты за месяц")
            p.contains("авто") || p.contains("машин") || p.contains("car") -> 
                Tuple6("CAR_MAINTENANCE", null, "#FF9100", "car", "Auto Expenses", "Расходы на авто")
            else -> 
                Tuple6("ALL", null, "#00E5FF", "chart", prompt.replaceFirstChar { it.uppercase() }, prompt.replaceFirstChar { it.uppercase() })
        }

        val newEntity = CustomDashboardEntity(
            title = titleEn,
            titleRu = titleRu,
            promptQuery = prompt,
            categoryFilter = category,
            subCategoryFilter = subCategory,
            timeRangeDays = timeDays,
            accentColorHex = color,
            iconName = icon
        )

        customDashboardDao.insertDashboard(newEntity)
    }

    fun getAuthToken(): String? {
        return sharedPrefs.getString("jwt_token", null)
    }

    fun getServerUrl(): String {
        return sharedPrefs.getString("server_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
    }

    fun setServerUrl(url: String) {
        sharedPrefs.edit().putString("server_url", url).apply()
    }

    fun getAppLanguage(): String {
        return sharedPrefs.getString("app_language", "EN") ?: "EN"
    }

    fun setAppLanguage(lang: String) {
        sharedPrefs.edit().putString("app_language", lang).apply()
    }

    private val apiService = ApiService(
        baseUrlProvider = { getServerUrl() },
        tokenProvider = { getAuthToken() }
    )
    
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

    suspend fun authenticateWithEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        apiService.authenticateWithEmail(email).map { response ->
            sharedPrefs.edit()
                .putString("jwt_token", response.access_token)
                .putString("user_email", response.user.email)
                .apply()
            
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

        // 2. Пробуем отправить на сервер асинхронно
        CoroutineScope(Dispatchers.IO).launch {
            trySyncEntry(entryId, text)
        }
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

private data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)
