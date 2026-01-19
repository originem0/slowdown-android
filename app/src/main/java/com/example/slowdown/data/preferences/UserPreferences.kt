package com.example.slowdown.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val DEFAULT_COUNTDOWN = intPreferencesKey("default_countdown")
        val COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
        val MIUI_AUTO_START_CONFIRMED = booleanPreferencesKey("miui_auto_start_confirmed")
        val MIUI_BACKGROUND_POPUP_CONFIRMED = booleanPreferencesKey("miui_background_popup_confirmed")
        val MIUI_BATTERY_SAVER_CONFIRMED = booleanPreferencesKey("miui_battery_saver_confirmed")
        val MIUI_LOCK_APP_CONFIRMED = booleanPreferencesKey("miui_lock_app_confirmed")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CUSTOM_REMINDER_TEXTS = stringPreferencesKey("custom_reminder_texts")
    }

    val serviceEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SERVICE_ENABLED] ?: false }

    val defaultCountdown: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[DEFAULT_COUNTDOWN] ?: 10 }

    val cooldownMinutes: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[COOLDOWN_MINUTES] ?: 5 }

    val miuiAutoStartConfirmed: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[MIUI_AUTO_START_CONFIRMED] ?: false }

    val miuiBackgroundPopupConfirmed: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[MIUI_BACKGROUND_POPUP_CONFIRMED] ?: false }

    val miuiBatterySaverConfirmed: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[MIUI_BATTERY_SAVER_CONFIRMED] ?: false }

    val miuiLockAppConfirmed: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[MIUI_LOCK_APP_CONFIRMED] ?: false }

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

    suspend fun setMiuiAutoStartConfirmed(confirmed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MIUI_AUTO_START_CONFIRMED] = confirmed
        }
    }

    suspend fun setMiuiBackgroundPopupConfirmed(confirmed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MIUI_BACKGROUND_POPUP_CONFIRMED] = confirmed
        }
    }

    suspend fun setMiuiBatterySaverConfirmed(confirmed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MIUI_BATTERY_SAVER_CONFIRMED] = confirmed
        }
    }

    suspend fun setMiuiLockAppConfirmed(confirmed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MIUI_LOCK_APP_CONFIRMED] = confirmed
        }
    }

    val appLanguage: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[APP_LANGUAGE] ?: "en" }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language
        }
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    // 自定义提醒文案（换行分隔多句）
    val customReminderTexts: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[CUSTOM_REMINDER_TEXTS] ?: "" }

    suspend fun setCustomReminderTexts(texts: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_REMINDER_TEXTS] = texts
        }
    }
}
