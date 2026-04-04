package com.example.healthguard.data.repo

import android.graphics.Bitmap
import android.util.Base64
import com.example.healthguard.data.GeminiOCRParser
import com.example.healthguard.data.models.MedicineOcrResult
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.MedicineScanRequest
import com.example.healthguard.data.network.dto.ScanSavedResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class VisionRepository(
    private val parser: GeminiOCRParser = GeminiOCRParser()
) {

    suspend fun ocr(bitmap: Bitmap, quality: Int = 85): MedicineOcrResult? {
        return try {
            val bytes = bitmap.asByteArray(quality)
            val body = bytes.toRequestBody("image/jpeg".toMediaType())


            val response = ApiClient.vision.scanMedicine(body)


            if (response.isSuccessful && response.body() != null) {
                val resultObject = response.body()!!


                parser.parseToResult(resultObject)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveScan(ocr: String, bitmap: Bitmap, quality: Int = 85): ScanSavedResponse {
        val req = MedicineScanRequest(
            ocrText = ocr,
            imageBase64 = bitmap.toBase64(quality)
        )
        return ApiClient.scan.saveScan(req)
    }


    private fun Bitmap.asByteArray(quality: Int): ByteArray =
        ByteArrayOutputStream().use { baos ->
            compress(Bitmap.CompressFormat.JPEG, quality, baos)
            baos.toByteArray()
        }

    private fun Bitmap.toBase64(quality: Int): String {
        val bytes = asByteArray(quality)
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
}