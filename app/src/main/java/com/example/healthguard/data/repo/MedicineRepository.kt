package com.example.healthguard.data.repo

import android.util.Log
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.GalinosResponse
import com.example.healthguard.data.network.dto.MedRecord
import com.example.healthguard.data.network.dto.MedService

class MedicineRepository {

    // Requires: ApiClient exposes `val med: MedService`
    private val service: MedService = ApiClient.med

    /**
     * Search medicines using raw OCR text.
     * We clean the text (remove dosages/forms etc.) before calling the backend.
     *
     * @param qRaw   Raw OCR text (possibly multiline/noisy).
     * @param lang   Optional language hint (e.g., "el", "en", "us").
     * @param source Optional source selector: "ema", "openfda", "both", "auto".
     */
    suspend fun searchMedicines(
        qRaw: String,
        lang: String? = null,
        source: String? = null
    ): List<MedRecord> {
        val cleaned = cleanOcrForSearch(qRaw)
        if (cleaned.isBlank()) return emptyList()

        return try {
            val resp = service.search(
                q = cleaned,
                lang = lang,
                source = source
            )
            resp.results
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Search Galinos for detailed Greek medicine information
     * Returns structured data with sections (indications, dosage, side effects, etc.)
     */
    suspend fun searchGalinos(query: String): GalinosResponse? {
        val cleaned = cleanOcrForSearch(query)
        if (cleaned.isBlank()) return null

        return try {
            service.searchGalinos(cleaned)
        } catch (e: Exception) {
            Log.e("MedicineRepository", "Galinos search failed", e)
            null
        }
    }

    // --------- OCR cleaning heuristics ---------
    /**
     * Turn noisy OCR text into a short brand/generic query.
     * Examples:
     *  "Augmentin 875/125 mg film-coated tablets" -> "augmentin"
     *  "Olanzapine 5 mg tablets"                 -> "olanzapine"
     */
    private fun cleanOcrForSearch(text: String): String {
        // Normalize whitespace
        var s = text
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        // Keep only the first ~6 tokens to avoid long paragraphs
        s = s.split(' ').take(6).joinToString(" ")

        // Remove pure dosage tokens & common form words
        s = s.replace(Regex("\\b\\d+[.,/]\\d+\\b"), "") // 875/125
            .replace(
                Regex("\\b\\d+\\s?(mg|ml|μg|mcg|g|%|mL)\\b", RegexOption.IGNORE_CASE),
                ""
            )
            .replace(
                Regex("\\b(tablets?|capsules?|film[- ]?coated|coated|dose|doses?)\\b", RegexOption.IGNORE_CASE),
                ""
            )
            .trim()

        // Strip leading/trailing punctuation
        s = s.trim { it.isWhitespace() || !it.isLetterOrDigit() }

        // Prefer first 1–2 meaningful tokens
        val tokens = s.split(' ')
            .map { it.trim() }
            .filter { it.length >= 2 }

        return tokens.take(2).joinToString(" ").lowercase()
    }
}