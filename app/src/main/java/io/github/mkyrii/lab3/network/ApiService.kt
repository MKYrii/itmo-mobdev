package io.github.mkyrii.lab3.network

import io.github.mkyrii.lab3.Message
import io.github.mkyrii.lab3.SendMessageRequest
import io.github.mkyrii.lab3.UserCredentials
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    @Headers("Content-Type: application/json")
    fun login(@Body credentials: UserCredentials): Call<String>

    @GET("channels")
    fun getChannels(): Call<List<String>>

    @GET("channel/{name}")
    fun getMessages(
        @Path("name") channelName: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: Int = 0,
        @Query("reverse") reverse: Boolean = false
    ): Call<List<Message>>

    @POST("messages")
    @Headers("Content-Type: application/json")
    fun sendMessage(@Body request: SendMessageRequest): Call<Void>

    @POST("logout")
    fun logout(): Call<Void>
}