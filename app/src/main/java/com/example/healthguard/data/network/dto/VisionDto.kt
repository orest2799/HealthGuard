package com.example.healthguard.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VisionDto(
    @Json(name = "text") val text: String? = null,
    @Json(name = "fullText") val fullText: String? = null,
    @Json(name = "words") val words: List<String>? = null
)
