package com.example.healthguard.data.network.service

import com.example.healthguard.data.network.dto.MedicineScanRequest
import com.example.healthguard.data.network.dto.ScanSavedResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ScanService {
    @POST("api/scan")
    suspend fun saveScan(@Body body: MedicineScanRequest): ScanSavedResponse
}