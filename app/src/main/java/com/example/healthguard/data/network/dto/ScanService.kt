package com.example.healthguard.data.network.dto

import retrofit2.http.Body
import retrofit2.http.POST

interface ScanService {
    @POST("api/vision/scan")
    suspend fun saveScan(@Body body: MedicineScanRequest): ScanSavedResponse
}
