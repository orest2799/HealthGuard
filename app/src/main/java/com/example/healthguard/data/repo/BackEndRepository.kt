package com.example.healthguard.data.repo

import com.example.healthguard.data.network.dto.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackendRepository {
    suspend fun checkHealth(): String = withContext(Dispatchers.IO) {
        val response = ApiClient.health.getHealth()
        (if (response.isSuccessful) response.body() ?: "Empty response"
        else "Error: ${response.code()} ${response.message()}") as String
    }
}

