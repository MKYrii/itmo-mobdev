package io.github.mkyrii.lab3.network

import io.github.mkyrii.lab3.storage.PreferencesManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val prefs: PreferencesManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = prefs.authToken

        val newRequest = if (token != null) {
            request.newBuilder()
                .header("X-Auth-Token", token)
                .build()
        } else {
            request
        }

        val response = chain.proceed(newRequest)

        if (response.code == 401) {
            prefs.clear()
            // Тут можно послать глобальное событие о 401
        }

        return response
    }
}