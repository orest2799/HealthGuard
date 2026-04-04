package com.example.healthguard.data.network.service

import com.example.healthguard.data.models.MedicineOcrResult
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface VisionService {
    @POST("api/ocr")
    suspend fun scanMedicine(@Body image: RequestBody): Response<MedicineOcrResult>
}