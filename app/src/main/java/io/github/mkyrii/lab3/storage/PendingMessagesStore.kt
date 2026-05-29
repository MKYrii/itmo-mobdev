package io.github.mkyrii.lab3.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.mkyrii.lab3.Message
import io.github.mkyrii.lab3.MessageData
import io.github.mkyrii.lab3.TextData

data class PendingMessage(
    val id: String,
    val channel: String,
    val from: String,
    val text: String,
    val time: Long
) {
    fun toMessage(): Message = Message(
        id = id,
        from = from,
        to = channel,
        data = MessageData(Text = TextData(text)),
        time = time
    )
}

class PendingMessagesStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<PendingMessage> {
        val json = prefs.getString(KEY_PENDING, null) ?: return emptyList()
        val type = object : TypeToken<List<PendingMessage>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getForChannel(channel: String): List<PendingMessage> =
        getAll().filter { it.channel == channel }

    fun add(message: PendingMessage) {
        val updated = getAll().toMutableList().apply { add(message) }
        save(updated)
    }

    fun remove(id: String) {
        val updated = getAll().filter { it.id != id }
        save(updated)
    }

    fun clear() {
        prefs.edit().remove(KEY_PENDING).apply()
    }

    private fun save(list: List<PendingMessage>) {
        prefs.edit().putString(KEY_PENDING, gson.toJson(list)).apply()
    }

    companion object {
        private const val PREFS_NAME = "chat_pending"
        private const val KEY_PENDING = "pending_messages"
    }
}
