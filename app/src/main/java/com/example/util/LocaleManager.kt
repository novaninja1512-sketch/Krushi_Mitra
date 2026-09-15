package com.example.util

import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LocaleManager {
    private const val PREFS_NAME = "krushi_locale_prefs"
    private const val KEY_LANG = "app_language"

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANG, "en") ?: "en"
        _currentLanguage.value = savedLang
    }

    fun setLanguage(context: Context, langCode: String) {
        _currentLanguage.value = langCode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, langCode).apply()
    }

    fun toggleLanguage(context: Context) {
        val next = if (_currentLanguage.value == "mr") "en" else "mr"
        setLanguage(context, next)
    }

    fun getLocalizedContext(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
