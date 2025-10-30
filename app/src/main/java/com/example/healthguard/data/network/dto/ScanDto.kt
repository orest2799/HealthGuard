package com.example.healthguard.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MedicineScanRequest(
    @Json(name = "ocr_text")     val ocr_text: String,
    @Json(name = "image_base64") val image_base64: String? = null
)

@JsonClass(generateAdapter = true)
data class ScanSavedResponse(
    @Json(name = "id")    val id: String,
    @Json(name = "saved") val saved: Boolean
)
