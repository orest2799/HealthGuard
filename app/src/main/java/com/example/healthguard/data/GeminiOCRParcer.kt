package com.example.healthguard.data

import android.util.Log
import com.example.healthguard.data.models.MedicineOcrResult
import java.text.Normalizer

class GeminiOCRParser {

    fun parseToResult(result: MedicineOcrResult): MedicineOcrResult? {
        return if (result.brand.isNullOrBlank() && result.activeSubstance.isNullOrBlank()) {
            Log.e("GeminiOCRParser", "Result is empty or invalid")
            null
        } else {
            result
        }
    }


    fun queryCandidates(dto: MedicineOcrResult): List<String> {
        val out = LinkedHashSet<String>()


        dto.brand?.let {
            out += it
            out += it.stripAccents().lowercase()
        }


        if (!dto.brand.isNullOrBlank() && !dto.strength.isNullOrBlank()) {
            out += "${dto.brand} ${dto.strength}"
        }


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