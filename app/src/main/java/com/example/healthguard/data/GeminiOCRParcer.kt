package com.example.healthguard.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.text.Normalizer

/**
 * Uses Gemini to parse messy OCR into a compact medicine object and
 * provides robust search query candidates (brand/generic/dosage variants).
 */
class GeminiOCRParser(
    private val apiKey: String
) {
    private val gemini by lazy {
        GenerativeModel(
            // Use a widely available model; change if you know 2.0 is enabled in your project
            modelName = "gemini-2.0-flash",
            apiKey = apiKey
        )
    }

    data class ParsedMedicine(
        val medicineName: String,      // brand/commercial name (e.g., "Augmentin", "DEPON")
        val dosage: String? = null,    // "500 mg", "875/125 mg", "10 mg/ml"
        val activeSubstance: String? = null // generic (e.g., "Paracetamol", "Αμοξικιλλίνη/Κλαβουλανικό οξύ")
    )

    /**
     * Parse OCR text to JSON with one retry if JSON is malformed.
     */
    suspend fun parseOCR(ocrText: String): ParsedMedicine? = withContext(Dispatchers.IO) {
        val cleaned = ocrText
            .replace('\u00A0', ' ')      // NBSP -> space
            .replace(Regex("[\\t\\r]"), " ")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(14)                    // keep it short; first lines carry brand/dosage
            .joinToString("\n")

        if (cleaned.length < 3) {
            Log.d(TAG, "OCR too short after cleaning")
            return@withContext null
        }

        val basePrompt = """
            You extract structured data from OCR text printed on a medicine box (Greek/English).
            Return **ONLY** compact JSON (no prose, no markdown, no comments). Exactly these keys:
            {
              "medicine_name": string,            // commercial/brand name like "DEPON", "Augmentin"
              "dosage": string | null,            // normalized with units if present (e.g., "500 mg", "875/125 mg")
              "active_substance": string | null   // generic/chemical name (e.g., "Paracetamol")
            }
            Rules:
            - Prefer the PRIMARY brand name (big, prominent word on box).
            - Keep original script (Greek stays Greek).
            - If a field is absent, set it to null (not empty string).
            - Do not include LOT/EXP/manufacturer or extra text.
            - Do not use markdown/code blocks.
            
            OCR:
            $cleaned
        """.trimIndent()

        // Try once, then retry with enforced JSON if parsing fails
        val attempts = listOf(
            basePrompt,
            "$basePrompt\nIMPORTANT: Output ONLY raw JSON. If previous output was invalid, FIX it."
        )

        for ((i, prompt) in attempts.withIndex()) {
            try {
                Log.d(TAG, "Sending to Gemini (attempt ${i + 1})…")
                val resp = gemini.generateContent(prompt)
                val text = (resp.text ?: "").trim()
                if (text.isEmpty()) {
                    Log.e(TAG, "Empty Gemini response")
                    continue
                }
                val jsonText = stripCodeFences(text)
                val obj = JSONObject(extractFirstJsonObject(jsonText))
                val name = obj.optString("medicine_name").takeIf { it.isNotBlank() && it != "null" } ?: continue
                val parsed = ParsedMedicine(
                    medicineName = name.trim(),
                    dosage = obj.optString("dosage").normalizeNull(),
                    activeSubstance = obj.optString("active_substance").normalizeNull()
                )
                Log.d(TAG, "Parsed: $parsed")
                return@withContext parsed
            } catch (e: JSONException) {
                Log.e(TAG, "JSON error (attempt ${i + 1}): ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Gemini call/parsing failed (attempt ${i + 1})", e)
            }
            // brief backoff
            delay(120L)
        }
        null
    }

    /**
     * Build robust search candidates from parsed data + raw OCR.
     * Use this list in order until you get a hit.
     */
    fun queryCandidates(parsed: ParsedMedicine?, rawOcr: String): List<String> {
        val out = LinkedHashSet<String>()

        // 1) Brand first (usually best for catalogs)
        parsed?.medicineName?.let { out += it }
        // Add brand + dosage combo if useful
        if (!parsed?.medicineName.isNullOrBlank() && !parsed?.dosage.isNullOrBlank()) {
            out += "${parsed!!.medicineName} ${parsed.dosage}"
        }

        // 2) Generic (active substance) — try raw and accent-stripped variants
        parsed?.activeSubstance?.let { gen ->
            out += gen
            out += gen.stripAccentsAndGreekFinalSigma().lowercase()
            // If compound “A/B”, try each side and a space-joined variant
            if ('/' in gen) {
                val parts = gen.split('/').map { it.trim() }.filter { it.length >= 3 }
                out.addAll(parts)
                out += parts.joinToString(" ")
                out += parts.joinToString(" ") { it.stripAccentsAndGreekFinalSigma().lowercase() }
            }
        }

        // 3) First meaningful line from OCR as a fallback
        rawOcr.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length >= 3 }?.let { out += it }

        // 4) If pattern hints at Augmentin (common case from your logs), add a brand fallback
        val raw = (parsed?.activeSubstance ?: rawOcr).lowercase()
        if (raw.contains("αμοξικ") && raw.contains("κλαβουλ")) {
            out += "Augmentin"
            if (!parsed?.dosage.isNullOrBlank()) out += "Augmentin ${parsed!!.dosage}"
        }

        // 5) Deduplicate, keep short but useful
        return out.map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.take(80) }
            .distinct()
            .toList()
    }

    // ---------------------- helpers ----------------------

    private fun String?.normalizeNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    private fun stripCodeFences(s: String): String =
        s.replace("```json", "").replace("```", "").trim()

    /**
     * Extract the first {...} JSON object from a response that may contain stray text.
     */
    private fun extractFirstJsonObject(s: String): String {
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start >= 0 && end > start) return s.substring(start, end + 1)
        return s // let JSONObject fail fast if not an object
    }

    /**
     * Remove accents and normalize Greek final sigma to sigma.
     */
    private fun String.stripAccentsAndGreekFinalSigma(): String {
        val nfd = Normalizer.normalize(this, Normalizer.Form.NFD)
        val noMarks = nfd.replace(Regex("\\p{Mn}+"), "")
        return noMarks
            .replace('ς', 'σ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val TAG = "GeminiOCRParser"
        fun create(apiKey: String): GeminiOCRParser = GeminiOCRParser(apiKey)
    }
}
