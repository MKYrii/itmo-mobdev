package io.github.mkyrii.lab3.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.mkyrii.lab3.Message

class LocalCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveChannels(channels: List<String>) {
        prefs.edit().putString(KEY_CHANNELS, gson.toJson(channels)).apply()
    }

    fun getChannels(): List<String>? {
        val json = prefs.getString(KEY_CHANNELS, null) ?: return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveMessages(channel: String, messages: List<Message>) {
        prefs.edit().putString(channelKey(channel), gson.toJson(messages)).apply()
    }

    fun getMessages(channel: String): List<Message>? {
        val json = prefs.getString(channelKey(channel), null) ?: return null
        val type = object : TypeToken<List<Message>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun channelKey(channel: String): String = "messages_${channel.replace("@", "_")}"

    companion object {
        private const val PREFS_NAME = "chat_cache"
        private const val KEY_CHANNELS = "channels"
    }
}
