package io.github.mkyrii.lab3.storage

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)

    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    var savedName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) {
            prefs.edit().putString(KEY_NAME, value).apply()
        }

    var savedPassword: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) {
            prefs.edit().putString(KEY_PASSWORD, value).apply()
        }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_NAME = "user_name"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}