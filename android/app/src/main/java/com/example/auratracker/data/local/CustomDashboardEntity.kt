package com.example.auratracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "custom_dashboards")
data class CustomDashboardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val titleRu: String,
    val promptQuery: String,
    val categoryFilter: String, // "FINANCE", "FITNESS", "CAR_MAINTENANCE", "ALL"
    val subCategoryFilter: String? = null, // e.g. "gas", "groceries", "pushups", "running"
    val timeRangeDays: Int = 30, // 7, 30, 365
    val accentColorHex: String = "#00E5FF",
    val iconName: String = "chart",
    val createdAt: Long = System.currentTimeMillis()
)
