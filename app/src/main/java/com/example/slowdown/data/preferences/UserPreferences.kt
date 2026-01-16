package com.example.slowdown.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val DEFAULT_COUNTDOWN = intPreferencesKey("default_countdown")
        val COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
    }

    val serviceEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SERVICE_ENABLED] ?: true }

    val defaultCountdown: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[DEFAULT_COUNTDOWN] ?: 10 }

    val cooldownMinutes: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[COOLDOWN_MINUTES] ?: 5 }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setDefaultCountdown(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_COUNTDOWN] = seconds
        }
    }

    suspend fun setCooldownMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[COOLDOWN_MINUTES] = minutes
        }
    }
}
