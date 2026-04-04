package com.example.healthguard.data.network.service

import retrofit2.Response
import retrofit2.http.GET

// 1. Data model for the request (Moshi will convert this to JSON)



data class HealthResponse(
    val status: String
)

interface HealthService {
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>


}