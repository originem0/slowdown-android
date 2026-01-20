package com.sharonZ.slowdown.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Helper class to programmatically change the app's locale at runtime.
 */
object LocaleHelper {

    /**
     * Updates the app's locale configuration.
     * Call this in Application.attachBaseContext() or Activity.attachBaseContext().
     * 
     * @param context The base context.
     * @param languageCode The language code (e.g., "en", "zh").
     * @return The updated context with the new locale.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.ENGLISH
        }
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }

    /**
     * Applies the locale and recreates the activity to reflect the change.
     * Call this when the user changes the language in settings.
     * 
     * @param activity The current activity.
     * @param languageCode The language code (e.g., "en", "zh").
     */
    fun applyLocaleAndRecreate(activity: Activity, languageCode: String) {
        setLocale(activity, languageCode)
        activity.recreate()
    }

    /**
     * Gets the current locale's language code from the Configuration.
     */
    fun getCurrentLanguage(context: Context): String {
        return context.resources.configuration.locales[0].language
    }
}
