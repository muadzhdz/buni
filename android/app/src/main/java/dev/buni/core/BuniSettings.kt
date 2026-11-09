package dev.buni.core

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

class BuniSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getWhitelistedPackages(): Set<String> {
        return prefs.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
    }

    fun setWhitelistedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_WHITELIST, packages).apply()
    }

    fun toggleAppWhitelist(packageName: String): Boolean {
        val current = getWhitelistedPackages().toMutableSet()
        val isNowWhitelisted = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        setWhitelistedPackages(current)
        return isNowWhitelisted
    }

    fun getUpstreamDns(): String {
        return prefs.getString(KEY_UPSTREAM_DNS, "1.1.1.1") ?: "1.1.1.1"
    }

    fun setUpstreamDns(dns: String) {
        prefs.edit().putString(KEY_UPSTREAM_DNS, dns).apply()
    }

    fun isFilterAdsEnabled(): Boolean {
        return prefs.getBoolean(KEY_FILTER_ADS, true)
    }

    fun setFilterAdsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FILTER_ADS, enabled).apply()
    }

    fun isFilterTrackersEnabled(): Boolean {
        return prefs.getBoolean(KEY_FILTER_TRACKERS, true)
    }

    fun setFilterTrackersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FILTER_TRACKERS, enabled).apply()
    }

    fun isFilterMalwareEnabled(): Boolean {
        return prefs.getBoolean(KEY_FILTER_MALWARE, true)
    }

    fun setFilterMalwareEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FILTER_MALWARE, enabled).apply()
    }

    fun getThemeMode(): ThemeMode {
        val modeStr = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        return try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun getLanguage(): Language {
        val langStr = prefs.getString(KEY_LANGUAGE, "EN") ?: "EN"
        return if (langStr == "ID") Language.ID else Language.EN
    }

    fun setLanguage(lang: Language) {
        prefs.edit().putString(KEY_LANGUAGE, lang.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "buni_user_settings"
        private const val KEY_WHITELIST = "pref_whitelisted_apps"
        private const val KEY_UPSTREAM_DNS = "pref_upstream_dns"
        private const val KEY_FILTER_ADS = "pref_filter_ads"
        private const val KEY_FILTER_TRACKERS = "pref_filter_trackers"
        private const val KEY_FILTER_MALWARE = "pref_filter_malware"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_THEME_MODE = "pref_theme_mode"
    }
}
