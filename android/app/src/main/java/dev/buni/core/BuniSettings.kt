package dev.buni.core

import android.content.Context
import android.content.SharedPreferences

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

    fun getWireGuardEndpoint(): String {
        return prefs.getString(KEY_WG_ENDPOINT, "") ?: ""
    }

    fun setWireGuardEndpoint(endpoint: String) {
        prefs.edit().putString(KEY_WG_ENDPOINT, endpoint).apply()
    }

    fun getWireGuardPublicKey(): String {
        return prefs.getString(KEY_WG_PUBKEY, "") ?: ""
    }

    fun setWireGuardPublicKey(pubKey: String) {
        prefs.edit().putString(KEY_WG_PUBKEY, pubKey).apply()
    }

    companion object {
        private const val PREFS_NAME = "buni_user_settings"
        private const val KEY_WHITELIST = "pref_whitelisted_apps"
        private const val KEY_UPSTREAM_DNS = "pref_upstream_dns"
        private const val KEY_FILTER_ADS = "pref_filter_ads"
        private const val KEY_FILTER_TRACKERS = "pref_filter_trackers"
        private const val KEY_FILTER_MALWARE = "pref_filter_malware"
        private const val KEY_WG_ENDPOINT = "pref_wg_endpoint"
        private const val KEY_WG_PUBKEY = "pref_wg_pubkey"
    }
}
