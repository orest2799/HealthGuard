package com.example.healthguard.data.repo

import com.example.healthguard.data.GeminiOCRParser
import com.example.healthguard.data.models.MedicineOcrResult
import com.example.healthguard.data.network.service.VisionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiOCRRepository(
    private val visionService: VisionService,
    private val parser: GeminiOCRParser = GeminiOCRParser()
) {
    suspend fun scanAndGetSearchTerms(imageBytes: ByteArray): MedicineOcrResult? = withContext(Dispatchers.IO) {
        try {
            val body = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())


            val response = visionService.scanMedicine(body)


            if (response.isSuccessful && response.body() != null) {
                val rawData = response.body()!!


                return@withContext rawData
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}