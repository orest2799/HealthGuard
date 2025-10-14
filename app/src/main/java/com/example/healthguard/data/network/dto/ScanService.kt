package com.example.healthguard.data.network.dto

import com.example.healthguard.data.network.MedicineScanRequest
import com.example.healthguard.data.network.ScanSavedResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ScanService {
    @POST("api/vision/scan")
    suspend fun saveScan(@Body body: MedicineScanRequest): ScanSavedResponse
}
