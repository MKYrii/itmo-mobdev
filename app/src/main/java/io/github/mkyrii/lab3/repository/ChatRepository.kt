package io.github.mkyrii.lab3.repository

import io.github.mkyrii.lab3.Message
import io.github.mkyrii.lab3.MessageDataContent
import io.github.mkyrii.lab3.SendMessageRequest
import io.github.mkyrii.lab3.TextContent
import io.github.mkyrii.lab3.UserCredentials
import io.github.mkyrii.lab3.network.ApiService
import io.github.mkyrii.lab3.network.WebSocketService
import io.github.mkyrii.lab3.storage.PreferencesManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatRepository(
    private val apiService: ApiService,
    private val prefs: PreferencesManager
) {

    fun login(
        name: String,
        password: String,
        onSuccess: (token: String) -> Unit,
        onError: (errorMsg: String) -> Unit
    ) {
        val credentials = UserCredentials(name, password)
        apiService.login(credentials).enqueue(object : Callback<String> {  // Изменено с LoginResponse на String
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.trim('"')  // Токен приходит как plain text
                    prefs.authToken = token
                    prefs.savedName = name
                    prefs.savedPassword = password
                    prefs.isLoggedIn = true
                    onSuccess(token)
                } else {
                    when (response.code()) {
                        401 -> onError("Неверный логин / пароль")
                        else -> onError("Ошибка: ${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                onError("Ошибка сети: ${t.message}")
            }
        })
    }

    fun getChannels(
        onSuccess: (channels: List<String>) -> Unit,
        onError: (errorMsg: String) -> Unit
    ) {
        apiService.getChannels().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!)
                } else {
                    handleError(response.code(), onError)
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                onError("Ошибка сети: ${t.message}")
            }
        })
    }

    fun getMessages(
        channelName: String,
        limit: Int = 20,
        lastKnownId: Int = 0,
        reverse: Boolean = false,
        onSuccess: (messages: List<Message>) -> Unit,
        onError: (errorMsg: String) -> Unit
    ) {
        apiService.getMessages(channelName, limit, lastKnownId, reverse)
            .enqueue(object : Callback<List<Message>> {
                override fun onResponse(call: Call<List<Message>>, response: Response<List<Message>>) {
                    if (response.isSuccessful && response.body() != null) {
                        onSuccess(response.body()!!)
                    } else {
                        handleError(response.code(), onError)
                    }
                }

                override fun onFailure(call: Call<List<Message>>, t: Throwable) {
                    onError("Ошибка сети: ${t.message}")
                }
            })
    }

    fun sendMessage(
        from: String,
        to: String,
        text: String,
        onSuccess: () -> Unit,
        onError: (errorMsg: String) -> Unit
    ) {
        val message = SendMessageRequest(
            from = from,
            to = to,
            data = MessageDataContent(TextContent(text))
        )

        apiService.sendMessage(message).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    onError("Ошибка: ${response.code()} - $errorBody")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                onError("Ошибка сети: ${t.message}")
            }
        })
    }

    fun logout(
        onSuccess: () -> Unit,
        onError: (errorMsg: String) -> Unit
    ) {
        apiService.logout().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                prefs.clear()
                onSuccess()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                prefs.clear()
                onSuccess()
            }
        })
    }

    fun isLoggedIn(): Boolean = prefs.isLoggedIn

    fun getSavedCredentials(): Pair<String?, String?> = Pair(prefs.savedName, prefs.savedPassword)

    private fun handleError(code: Int, onError: (String) -> Unit) {
        when (code) {
            401 -> onError("Не авторизован")
            else -> onError("Ошибка: $code")
        }
    }

    // Добавь поле
    private var webSocketService: WebSocketService? = null

    // Добавь метод инициализации WebSocket
    fun initWebSocket(onNewMessage: (Message) -> Unit, onUnauthorized: () -> Unit) {
        val username = prefs.savedName ?: return
        webSocketService = WebSocketService(prefs, onNewMessage, onUnauthorized)
        webSocketService?.connect(username)
    }

    fun closeWebSocket() {
        webSocketService?.disconnect()
        webSocketService = null
    }

    fun sendStartTyping(chat: String) {
        webSocketService?.sendStartTyping(chat)
    }

    fun sendEndTyping() {
        webSocketService?.sendEndTyping()
    }

    // Отправка сообщения через WebSocket (для реального времени)
    fun sendMessageViaWebSocket(to: String, text: String) {
        webSocketService?.sendTextMessage(to, text)
    }
}