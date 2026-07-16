package com.example.auratracker

import android.app.Application
import com.example.auratracker.data.local.AppDatabase
import com.example.auratracker.data.repository.LogRepository

class AuraTrackerApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { LogRepository(this, database.logDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
