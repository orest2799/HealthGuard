// data/network/dto/ChatService.kt
package com.example.healthguard.data.network.dto

import retrofit2.http.Body
import retrofit2.http.POST

interface ChatService {

    @POST("chat")
    suspend fun chat(@Body req: ChatRequest): ChatResponse
}
