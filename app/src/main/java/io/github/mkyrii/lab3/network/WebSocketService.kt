package io.github.mkyrii.lab3.network

import android.util.Log
import io.github.mkyrii.lab3.Message
import io.github.mkyrii.lab3.storage.PreferencesManager
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketService(
    private val prefs: PreferencesManager,
    private val onNewMessage: (Message) -> Unit,
    private val onUnauthorized: () -> Unit
) {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null

    fun connect(username: String) {
        val token = prefs.authToken ?: return
        val url = "wss://faerytea.name/ws/$username?token=$token"

        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        Log.d("WebSocket", "Connecting... Username: $username, Token: $token")

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
                Log.d("WebSocket", "Connecting... Username: $username, Token: $token")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = com.google.gson.JsonParser.parseString(text).asJsonObject
                    if (json.has("NewMessage")) {
                        val msgObj = json.getAsJsonObject("NewMessage").getAsJsonObject("msg")
                        val gson = com.google.gson.Gson()
                        val message = gson.fromJson(msgObj, Message::class.java)
                        onNewMessage(message)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Parse error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Failure: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $reason")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        client?.dispatcher?.executorService?.shutdown()
        client = null
    }

    fun sendStartTyping(chat: String) {
        val json = """{"StartTyping":{"chat":"$chat"}}"""
        webSocket?.send(json)
    }

    fun sendEndTyping() {
        val json = """{"EndTyping":{}}"""
        webSocket?.send(json)
    }

    fun sendTextMessage(to: String, text: String) {
        val json = """{"NewMessageText":{"to":"$to","text":"$text"}}"""
        webSocket?.send(json)
    }

    fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WebSocket", "Connected successfully")
    }

    fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("WebSocket", "Received: $text")
        // остальной код
    }

    fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("WebSocket", "Failed: ${t.message}")
    }
}