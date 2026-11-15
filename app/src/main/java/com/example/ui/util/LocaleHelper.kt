package com.example.ui.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Utility to manage and switch application language and locale dynamically at runtime.
 * Supports French (fr), English (en), Russian (ru), Spanish (es), Arabic (ar), etc.
 */
object LocaleHelper {

    private const val PREFS_NAME = "adt_pro_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language_code"

    /**
     * Maps our app internal language code (FR, BM, ES, ZH, RU, EN, FRANGLAIS, AR) to a Java Locale.
     */
    fun mapCodeToLocale(code: String?): Locale {
        return when (code?.uppercase()) {
            "FR", "FRANGLAIS" -> Locale("fr", "FR")
            "BM", "BAMBARA" -> Locale("bm", "ML")
            "ES", "SPANISH" -> Locale("es", "ES")
            "ZH", "CHINESE", "CN" -> Locale("zh", "CN")
            "RU", "RUSSIAN", "RUSSIA" -> Locale("ru", "RU")
            "EN" -> Locale("en", "US")
            "AR" -> Locale("ar", "SA")
            else -> Locale("fr", "FR")
        }
    }

    /**
     * Returns the ISO language code (e.g. "fr", "bm", "es", "zh", "ru", "en") from the given app code.
     */
    fun getIsoLanguage(code: String?): String {
        return when (code?.uppercase()) {
            "FR", "FRANGLAIS" -> "fr"
            "BM", "BAMBARA" -> "bm"
            "ES", "SPANISH" -> "es"
            "ZH", "CHINESE", "CN" -> "zh"
            "RU", "RUSSIAN", "RUSSIA" -> "ru"
            "EN" -> "en"
            "AR" -> "ar"
            else -> "fr"
        }
    }

    /**
     * Persists the selected language code in SharedPreferences for cold starts.
     */
    fun persistLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /**
     * Gets the currently persisted language code, defaulting to "FR".
     */
    fun getPersistedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "FR") ?: "FR"
    }

    /**
     * Sets the application locale at runtime and updates the resources configuration.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        persistLanguage(context, languageCode)
        val locale = mapCodeToLocale(languageCode)
        Locale.setDefault(locale)

        return updateResources(context, locale)
    }

    /**
     * Applies the locale to an activity immediately, recreating it or updating configuration.
     */
    fun applyLocale(activity: Activity, languageCode: String) {
        persistLanguage(activity, languageCode)
        val locale = mapCodeToLocale(languageCode)
        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = activity.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                localeManager?.applicationLocales = LocaleList(locale)
            } catch (_: Exception) {
                // Fallback to configuration update
            }
        }

        updateResources(activity, locale)
        updateResources(activity.applicationContext, locale)
    }

    /**
     * Wraps context with the updated locale configuration for attachBaseContext().
     */
    fun wrap(context: Context, languageCode: String? = null): ContextWrapper {
        val code = languageCode ?: getPersistedLanguage(context)
        val locale = mapCodeToLocale(code)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        val newContext = context.createConfigurationContext(config)
        return ContextWrapper(newContext)
    }

    @Suppress("DEPRECATION")
    private fun updateResources(context: Context, locale: Locale): Context {
        val res: Resources = context.resources
        val config: Configuration = Configuration(res.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            res.updateConfiguration(config, res.displayMetrics)
            context
        }
    }
}
