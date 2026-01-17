package com.example.slowdown.data.repository

import com.example.slowdown.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing the app's language preference.
 * Provides a Flow for observing the current language and a suspend function to update it.
 */
class LanguageRepository(
    private val userPreferences: UserPreferences
) {
    /**
     * Flow that emits the current app language ("en" or "zh").
     * Default is "en" (English).
     */
    val appLanguage: Flow<String> = userPreferences.appLanguage

    /**
     * Sets the app language.
     * @param language The language code, either "en" (English) or "zh" (Chinese).
     */
    suspend fun setAppLanguage(language: String) {
        userPreferences.setAppLanguage(language)
    }
}
