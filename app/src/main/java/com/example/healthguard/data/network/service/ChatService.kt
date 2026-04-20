package com.example.healthguard.data.network.service

import com.example.healthguard.data.network.dto.ChatRequest
import com.example.healthguard.data.network.dto.ChatResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ChatService {
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    @Headers("Content-Type: application/octet-stream")
    @POST("api/chat/from-image")
    suspend fun chatFromImage(
        @Body body: RequestBody,
        @Header("X-Chat-Language") language: String
    ): Response<ChatResponse>
}
