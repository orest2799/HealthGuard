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

    /**
     * Updated to handle the String response from the API and
     * return a parsed MedicineOcrResult object.
     */
    suspend fun ocr(bitmap: Bitmap, quality: Int = 85): MedicineOcrResult? {
        val bytes = bitmap.asByteArray(quality)
        val body = bytes.toRequestBody("image/jpeg".toMediaType())

        // 1. Get the raw String from the API
        val rawJson: MedicineOcrResult = ApiClient.vision.scanMedicine(body)

        // 2. Use the parser to convert String -> MedicineOcrResult
        // This fixes the "Return type mismatch"
        return parser.parseToResult(rawJson)
    }

    suspend fun saveScan(ocr: String, bitmap: Bitmap, quality: Int = 85): ScanSavedResponse {
        val req = MedicineScanRequest(
            ocrText = ocr,
            imageBase64 = bitmap.toBase64(quality)
        )
        return ApiClient.scan.saveScan(req)
    }

    /* ------------ helpers ------------ */

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