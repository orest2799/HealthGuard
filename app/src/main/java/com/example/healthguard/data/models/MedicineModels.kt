package com.example.healthguard.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true) // Απαραίτητο για το Moshi
data class MedicineOcrResult(
    @Json(name = "brand") val brand: String? = null,
    @Json(name = "activeSubstance") val activeSubstance: String? = null,
    @Json(name = "strength") val strength: String? = null,
    @Json(name = "form") val form: String? = null
)