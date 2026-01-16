package com.example.slowdown

import android.app.Application
import com.example.slowdown.data.local.AppDatabase
import com.example.slowdown.data.preferences.UserPreferences
import com.example.slowdown.data.repository.SlowDownRepository

class SlowDownApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
    val repository: SlowDownRepository by lazy {
        SlowDownRepository(
            interventionDao = database.interventionDao(),
            monitoredAppDao = database.monitoredAppDao(),
            userPreferences = userPreferences
        )
    }
}
