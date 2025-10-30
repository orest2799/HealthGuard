package com.example.healthguard.data.remote

import android.graphics.Bitmap
import com.example.healthguard.data.network.dto.MedicineScanRequest
import com.example.healthguard.data.network.dto.ScanSavedResponse
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.VisionDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class VisionRepository {

    suspend fun ocr(bitmap: Bitmap, quality: Int = 85): VisionDto {
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = "crop.jpg",
            body = bitmap.asJpeg(quality)
        )
        return ApiClient.vision.annotate(part)
    }

    suspend fun saveScan(ocr: String, bitmap: Bitmap, quality: Int = 85): ScanSavedResponse {
        val req = MedicineScanRequest(
            ocr_text = ocr,
            image_base64 = bitmap.toBase64(quality)
        )
        return ApiClient.scan.saveScan(req)
    }

    /* ------------ helpers ------------ */

    private fun Bitmap.asJpeg(quality: Int) =
        ByteArrayOutputStream().use { baos ->
            compress(Bitmap.CompressFormat.JPEG, quality, baos)
            baos.toByteArray().toRequestBody("image/jpeg".toMediaType())
        }

    private fun Bitmap.toBase64(quality: Int): String {
        val baos = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)
    }
}

