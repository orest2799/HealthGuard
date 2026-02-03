package com.example.healthguard.data.repo

import VisionService
import com.example.healthguard.data.GeminiOCRParser
import com.example.healthguard.data.models.MedicineOcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiOCRRepository(
    private val visionService: VisionService,
    private val parser: GeminiOCRParser = GeminiOCRParser()
) {
    // This name MUST match what you call in CameraScreen.kt
    suspend fun scanAndGetSearchTerms(imageBytes: ByteArray): MedicineOcrResult? = withContext(Dispatchers.IO) {
        try {
            val body = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val result = visionService.scanMedicine(body) // This returns the raw JSON string

            // This now resolves because we added it to the parser above
            parser.parseToResult(result)
        } catch (e: Exception) {
            null
        }
    }
}