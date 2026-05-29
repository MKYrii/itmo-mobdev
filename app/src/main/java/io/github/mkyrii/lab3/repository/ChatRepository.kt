package io.github.mkyrii.lab3.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import io.github.mkyrii.lab3.Message
import io.github.mkyrii.lab3.storage.LocalCache
import io.github.mkyrii.lab3.storage.PendingMessage
import io.github.mkyrii.lab3.storage.PendingMessagesStore
import io.github.mkyrii.lab3.storage.PreferencesManager
import io.github.mkyrii.lab3.util.MessageMerge
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class ChatRepository(
    private val context: Context,
    private val prefs: PreferencesManager
) {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val cache = LocalCache(context)
    private val pendingStore = PendingMessagesStore(context)

    private var webSocket: WebSocket? = null
    private var onNewMessageCallback: ((Message) -> Unit)? = null
    private var onPendingFlushComplete: (() -> Unit)? = null

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun setOnPendingFlushComplete(callback: () -> Unit) {
        onPendingFlushComplete = callback
    }

    fun login(
        name: String,
        password: String,
        onSuccess: (token: String) -> Unit,
        onError: (errorMsg: String) -> Unit
    ) {
        val json = "{\"name\":\"$name\",\"pwd\":\"$password\"}"
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://faerytea.name/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val token = responseBody.trim('"')
                    prefs.authToken = token
                    prefs.savedName = name
                    prefs.savedPassword = password
                    prefs.isLoggedIn = true
                    onSuccess(token)
                    Log.d("LOGIN", "Вызываем connectWebSocket для $name")
                    connectWebSocket(name)
                } else {
                    when (response.code) {
                        401 -> onError("Неверный логин / пароль")
                        else -> onError("Ошибка: ${response.code}")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onError("Ошибка сети: ${e.message}")
            }
        })
    }

    fun connectWebSocket(username: String) {
        if (!isOnline()) return

        val token = prefs.authToken
        if (token == null) {
            Log.e("WebSocket", "Нет токена")
            return
        }

        webSocket?.close(1000, "Reconnect")
        webSocket = null

        val url = "wss://faerytea.name/ws/$username?token=$token"
        val request = Request.Builder()
            .url(url)
            .build()

        val wsClient = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", " Подключено")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Получено: $text")
                try {
                    val json = com.google.gson.JsonParser.parseString(text).asJsonObject

                    if (json.has("NewMessage") && !json.get("NewMessage").isJsonNull) {
                        val msgObj = json.getAsJsonObject("NewMessage").getAsJsonObject("msg")
                        val message = gson.fromJson(msgObj, Message::class.java)
                        appendMessageToCache(message)
                        onNewMessageCallback?.invoke(message)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Ошибка парсинга: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Ошибка: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Закрыто: $reason")
            }
        })
    }

    fun reconnectIfLoggedIn() {
        val name = prefs.savedName ?: return
        if (isLoggedIn() && isOnline()) {
            connectWebSocket(name)
            flushPendingMessages()
        }
    }

    fun closeWebSocket() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    fun sendStartTyping(chat: String) {
        if (!isOnline()) return
        val json = """{"StartTyping":{"chat":"$chat"}}"""
        webSocket?.send(json)
    }

    fun sendEndTyping() {
        if (!isOnline()) return
        val json = """{"EndTyping":{}}"""
        webSocket?.send(json)
    }

    fun setOnNewMessageCallback(callback: (Message) -> Unit) {
        onNewMessageCallback = callback
    }

    fun getChannels(
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val cached = cache.getChannels()
        if (!isOnline()) {
            if (cached != null) {
                onSuccess(cached)
            } else {
                onError("Нет подключения к сети")
            }
            return
        }

        val token = prefs.authToken ?: run { onError("Нет токена"); return }

        val request = Request.Builder()
            .url("https://faerytea.name/channels")
            .get()
            .header("X-Auth-Token", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val channels = gson.fromJson(body, Array<String>::class.java).toList()
                    cache.saveChannels(channels)
                    onSuccess(channels)
                } else if (cached != null) {
                    onSuccess(cached)
                } else {
                    onError("Ошибка: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (cached != null) {
                    onSuccess(cached)
                } else {
                    onError("Ошибка сети: ${e.message}")
                }
            }
        })
    }

    fun getMessages(
        channelName: String,
        limit: Int = 20,
        lastKnownId: Int = 99999999,
        reverse: Boolean = false,
        onSuccess: (List<Message>) -> Unit,
        onError: (String) -> Unit
    ) {
        val cachedDisplay = getDisplayMessages(channelName)

        if (!isOnline()) {
            if (cachedDisplay.isNotEmpty()) {
                onSuccess(cachedDisplay)
            } else {
                onError("Нет подключения к сети")
            }
            return
        }

        val token = prefs.authToken ?: run { onError("Нет токена"); return }

        val url = "https://faerytea.name/channel/$channelName?limit=$limit&lastKnownId=$lastKnownId&reverse=true"
        Log.d("GET_MESSAGES", "URL: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .header("X-Auth-Token", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val messages = gson.fromJson(body, Array<Message>::class.java).toList()
                    Log.d("GET_MESSAGES", "Получено сообщений: ${messages.size}")
                    val merged = mergeWithPending(channelName, messages)
                    if (lastKnownId >= 9999999) {
                        cache.saveMessages(channelName, merged)
                    } else {
                        cache.saveMessages(
                            channelName,
                            MessageMerge.sortChronologically(
                                cache.getMessages(channelName).orEmpty() + merged
                            )
                        )
                    }
                    onSuccess(merged)
                } else if (cachedDisplay.isNotEmpty()) {
                    onSuccess(cachedDisplay)
                } else {
                    onError("Ошибка: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (cachedDisplay.isNotEmpty()) {
                    onSuccess(cachedDisplay)
                } else {
                    onError("Ошибка сети: ${e.message}")
                }
            }
        })
    }

    fun getDisplayMessages(channelName: String): List<Message> {
        val cached = cache.getMessages(channelName).orEmpty()
        val pending = pendingStore.getForChannel(channelName).map { it.toMessage() }
        return MessageMerge.merge(cached, pending)
    }

    fun sendMessage(
        from: String,
        to: String,
        text: String,
        onSuccess: () -> Unit,
        onQueued: (Message) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isOnline()) {
            val pending = PendingMessage(
                id = "pending_${UUID.randomUUID()}",
                channel = to,
                from = from,
                text = text,
                time = System.currentTimeMillis() / 1000
            )
            pendingStore.add(pending)
            val message = pending.toMessage()
            appendMessageToCache(message)
            onQueued(message)
            return
        }

        sendMessageToServer(from, to, text, onSuccess, onError)
    }

    fun flushPendingMessages() {
        if (!isOnline()) return
        val snapshot = pendingStore.getAll().toList()
        if (snapshot.isEmpty()) return
        sendPendingSnapshot(snapshot, 0)
    }

    private fun sendPendingSnapshot(snapshot: List<PendingMessage>, index: Int) {
        if (index >= snapshot.size) {
            onPendingFlushComplete?.invoke()
            return
        }

        val item = snapshot[index]
        sendMessageToServer(
            from = item.from,
            to = item.channel,
            text = item.text,
            onSuccess = {
                pendingStore.remove(item.id)
                sendPendingSnapshot(snapshot, index + 1)
            },
            onError = {
                onPendingFlushComplete?.invoke()
            }
        )
    }

    private fun sendMessageToServer(
        from: String,
        to: String,
        text: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val token = prefs.authToken ?: run { onError("Нет токена"); return }

        val message = mapOf(
            "from" to from,
            "to" to to,
            "data" to mapOf("Text" to mapOf("text" to text))
        )

        val json = gson.toJson(message)
        Log.d("SEND", "Отправляем JSON: $json")
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://faerytea.name/messages")
            .post(body)
            .header("X-Auth-Token", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("SEND", "Ответ: код ${response.code}, тело: $responseBody")
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Ошибка: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onError("Ошибка сети: ${e.message}")
            }
        })
    }

    private fun mergeWithPending(channelName: String, serverMessages: List<Message>): List<Message> {
        val pending = pendingStore.getForChannel(channelName).map { it.toMessage() }
        return MessageMerge.merge(serverMessages, pending)
    }

    private fun appendMessageToCache(message: Message) {
        val channelKey = message.to
        val existing = cache.getMessages(channelKey).orEmpty()
        if (existing.any { it.id == message.id }) return
        cache.saveMessages(channelKey, MessageMerge.sortChronologically(existing + message))
    }

    fun logout(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val token = prefs.authToken ?: run { onError("Нет токена"); return }

        val request = Request.Builder()
            .url("https://faerytea.name/logout")
            .post("".toRequestBody())
            .header("X-Auth-Token", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                clearLocalData()
                onSuccess()
            }

            override fun onFailure(call: Call, e: IOException) {
                clearLocalData()
                onSuccess()
            }
        })
    }

    private fun clearLocalData() {
        closeWebSocket()
        prefs.clear()
        cache.clearAll()
        pendingStore.clear()
    }

    fun isLoggedIn(): Boolean = prefs.isLoggedIn
    fun getSavedCredentials(): Pair<String?, String?> = Pair(prefs.savedName, prefs.savedPassword)
}
