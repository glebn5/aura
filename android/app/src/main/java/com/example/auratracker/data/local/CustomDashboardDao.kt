package com.example.auratracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomDashboardDao {
    @Query("SELECT * FROM custom_dashboards ORDER BY createdAt ASC")
    fun getAllDashboardsFlow(): Flow<List<CustomDashboardEntity>>

    @Query("SELECT COUNT(*) FROM custom_dashboards")
    suspend fun getDashboardsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: CustomDashboardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dashboards: List<CustomDashboardEntity>)

    @Query("DELETE FROM custom_dashboards WHERE id = :id")
    suspend fun deleteDashboardById(id: String)
}
