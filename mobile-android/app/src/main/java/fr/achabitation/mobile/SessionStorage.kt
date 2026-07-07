package fr.achabitation.mobile

import android.content.SharedPreferences

interface SessionStorage {
    fun getString(key: String, defaultValue: String? = null): String?
    fun putString(key: String, value: String)
    fun remove(vararg keys: String)
}

class SharedPreferencesSessionStorage(
    private val prefs: SharedPreferences
) : SessionStorage {
    override fun getString(key: String, defaultValue: String?): String? = prefs.getString(key, defaultValue)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(vararg keys: String) {
        val editor = prefs.edit()
        keys.forEach { editor.remove(it) }
        editor.apply()
    }
}
