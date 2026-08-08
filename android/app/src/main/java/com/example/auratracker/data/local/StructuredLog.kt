package com.example.auratracker.data.local

import kotlinx.serialization.Serializable

@Serializable
data class FinanceData(
    val amount: Double,
    val currency: String = "RUB",
    val item: String,
    val category: String
)

@Serializable
data class FitnessData(
    val activity_type: String,
    val distance_km: Double? = null,
    val duration_minutes: Double? = null,
    val intensity_level: String? = null,
    val reps: Int? = null
)

@Serializable
data class CarMaintenanceData(
    val part_or_service: String,
    val cost: Double? = null,
    val currency: String = "RUB"
)

@Serializable
data class RoutineData(
    val activity: String,
    val duration_hours: Double? = null
)

@Serializable
data class GeminiStructuredLog(
    val category: String, // FINANCE, FITNESS, CAR_MAINTENANCE, ROUTINE, OTHER
    val finance_data: FinanceData? = null,
    val fitness_data: FitnessData? = null,
    val car_data: CarMaintenanceData? = null,
    val routine_data: RoutineData? = null,
    val other_summary: String? = null
)
