package com.example.healthguard.data.models

import com.google.gson.annotations.SerializedName

data class MedicineOcrResult(
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("activeSubstance") val activeSubstance: String? = null,
    @SerializedName("strength") val strength: String? = null,
    @SerializedName("form") val form: String? = null
)