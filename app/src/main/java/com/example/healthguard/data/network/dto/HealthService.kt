package com.example.healthguard.data.network.dto

import retrofit2.Response
import retrofit2.http.GET

interface HealthService {
    @GET("health")
    suspend fun getHealth(): Response<String>
}

