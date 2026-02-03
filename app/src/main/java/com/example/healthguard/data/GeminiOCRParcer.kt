package com.example.healthguard.data

import android.util.Log
import com.example.healthguard.data.models.MedicineOcrResult
import java.text.Normalizer

class GeminiOCRParser {

    /**
     * Step 1: Validates the result.
     * Since Retrofit/GSON already parsed the JSON into an object,
     * we don't need substringAfter or fromJson anymore.
     */
    fun parseToResult(result: MedicineOcrResult): MedicineOcrResult? {
        return if (result.brand.isNullOrBlank() && result.activeSubstance.isNullOrBlank()) {
            Log.e("GeminiOCRParser", "Result is empty or invalid")
            null
        } else {
            result
        }
    }

    /**
     * Step 2: Creates search terms for your local database.
     */
    fun queryCandidates(dto: MedicineOcrResult): List<String> {
        val out = LinkedHashSet<String>()

        // Add Brand
        dto.brand?.let {
            out += it
            out += it.stripAccents().lowercase()
        }

        // Add Brand + Strength combo
        if (!dto.brand.isNullOrBlank() && !dto.strength.isNullOrBlank()) {
            out += "${dto.brand} ${dto.strength}"
        }

        // Add Active Substances
        dto.activeSubstance?.let { gen ->
            out += gen
            out += gen.stripAccents().lowercase()
            if ('/' in gen) {
                val parts = gen.split('/').map { it.trim() }
                out.addAll(parts)
            }
        }

        return out.filter { it.length >= 3 }.toList()
    }

    private fun String.stripAccents(): String {
        val nfd = Normalizer.normalize(this, Normalizer.Form.NFD)
        return nfd.replace(Regex("\\p{Mn}+"), "").replace('ς', 'σ').trim()
    }
}