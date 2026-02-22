package fr.flal.navipk.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("navipk_prefs", Context.MODE_PRIVATE)

    fun save(serverUrl: String, username: String, password: String) {
        prefs.edit()
            .putString("server_url", serverUrl)
            .putString("username", username)
            .putString("password", password)
            .apply()
    }

    fun getServerUrl(): String = prefs.getString("server_url", "") ?: ""
    fun getUsername(): String = prefs.getString("username", "") ?: ""
    fun getPassword(): String = prefs.getString("password", "") ?: ""

    fun isLoggedIn(): Boolean {
        return getServerUrl().isNotBlank() && getUsername().isNotBlank() && getPassword().isNotBlank()
    }

    fun getMaxCacheSizeMb(): Int = prefs.getInt("max_cache_size_mb", 1024)

    fun setMaxCacheSizeMb(sizeMb: Int) {
        prefs.edit().putInt("max_cache_size_mb", sizeMb).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
