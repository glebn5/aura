package com.example.auratracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM log_entries ORDER BY createdAt DESC")
    fun getAllLogsFlow(): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPendingLogs(): List<LogEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: LogEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LogEntryEntity>)

    @Update
    suspend fun updateLog(entry: LogEntryEntity)

    @Query("UPDATE log_entries SET syncStatus = :status, category = :category, structuredDataJson = :json WHERE id = :id")
    suspend fun updateSyncSuccess(id: String, status: String, category: String, json: String)

    @Query("DELETE FROM log_entries WHERE id = :id")
    suspend fun deleteLogById(id: String)

    @Query("DELETE FROM log_entries")
    suspend fun clearAllLogs()
}
