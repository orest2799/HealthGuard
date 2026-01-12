package com.example.healthguard.data.repo

import android.util.Log
import com.example.healthguard.data.GeminiOCRParser
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.ChatRequest
import com.example.healthguard.data.network.dto.ChatResponse
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.data.network.dto.MedRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatRepository(
    private val geminiParser: GeminiOCRParser? = null
) {

    companion object {
        private const val TAG = "ChatRepository"
    }

    /**
     * Complete flow: Image → OCR → Medicine Search → Chat
     * This replaces the non-existent /api/analyze-ocr endpoint
     */
    suspend fun processMedicineImage(imageFile: File): ChatSessionResult =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Step 1: Starting OCR for ${imageFile.name}")

                // Step 1: Extract text from image using Vision API
                val ocrResult = ApiClient.vision.annotate(
                    MultipartBody.Part.createFormData(
                        "file",
                        imageFile.name,
                        imageFile.asRequestBody("image/*".toMediaType())
                    )
                )

                // ✅ FIX: VisionDto only has "text"
                val extractedText = ocrResult.text.orEmpty()
                Log.d(TAG, "OCR extracted: ${extractedText.take(200)}...")

                if (extractedText.isBlank()) {
                    return@withContext ChatSessionResult.Error("Δεν βρέθηκε κείμενο στην εικόνα")
                }

                // Continue pipeline with OCR text
                processMedicineWithOCR(extractedText)

            } catch (e: Exception) {
                Log.e(TAG, "Error in processMedicineImage", e)
                ChatSessionResult.Error("Σφάλμα: ${e.message}")
            }
        }

    suspend fun processMedicineWithOCR(
        ocrText: String,
        medicineName: String? = null
    ): ChatSessionResult = withContext(Dispatchers.IO) {
        try {
            Log.e(TAG, "═══════════════════════════════════════")
            Log.e(TAG, "🔍 DIAGNOSTIC CHECK")
            Log.e(TAG, "═══════════════════════════════════════")
            Log.e(TAG, "Is geminiParser null? ${geminiParser == null}")
            Log.e(TAG, "medicineName param: $medicineName")
            Log.e(TAG, if (geminiParser == null) "❌❌❌ GEMINI PARSER IS NULL!" else "✅✅✅ GEMINI PARSER EXISTS!")
            Log.e(TAG, "═══════════════════════════════════════")

            Log.d(TAG, "Processing with pre-extracted OCR")
            Log.d(TAG, "OCR text: ${ocrText.take(200)}")

            // 1) Parse with Gemini if available (even if medicineName is provided — we want brand/dosage)
            val parsedMedicine = if (geminiParser != null) {
                runCatching {
                    geminiParser.parseOCR(ocrText)
                }.onFailure { Log.e(TAG, "Gemini parse failed", it) }.getOrNull()
            } else null

            // 2) Build candidate queries (brand first)
            val candidates = buildCandidatesForSearch(
                ocrText = ocrText,
                parsed = parsedMedicine,
                preferredName = medicineName // can be null
            )
            Log.d(TAG, "Candidates -> ${candidates.joinToString(" | ")}")

            // 3) Try sources in a sensible order
            val sourcesToTry = listOf("galinos", "both") // adjust to match your backend
            val languagesToTry = listOf("el", null)      // try Greek, then default

            var found: List<MedRecord> = emptyList()
            var usedQuery: String? = null

            loop@ for (q in candidates) {
                for (lang in languagesToTry) {
                    for (src in sourcesToTry) {
                        try {
                            Log.d(TAG, "Searching q='$q', lang='${lang ?: "-"}', source='$src'")
                            val resp = ApiClient.med.search(q = q.take(80), lang = lang, source = src)
                            if (resp.results.isNotEmpty()) {
                                found = resp.results
                                usedQuery = q
                                Log.d(TAG, "Found ${found.size} medicines for '$q' (lang=${lang ?: "-"}, source=$src)")
                                break@loop
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Search error for '$q' (lang=${lang ?: "-"}, source=$src)", e)
                        }
                    }
                }
            }

            if (found.isEmpty()) {
                return@withContext ChatSessionResult.Error(
                    "Δεν βρέθηκε το φάρμακο. Δοκιμάστηκαν: ${candidates.joinToString(" | ")}"
                )
            }

            // 4) If we have parsed data and multiple results, pick the best by your scorer
            val best = if (parsedMedicine != null && found.size > 1) {
                findBestMatch(found, parsedMedicine) ?: found.first()
            } else found.first()

            val medQuery = best.brand ?: best.generic ?: usedQuery ?: candidates.first()
            Log.d(TAG, "✅ Best/Used medicine: $medQuery")

            continueWithMedicines(
                medicines = listOf(best),
                medQuery = medQuery,
                ocrText = ocrText,
                parsed = parsedMedicine
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in processMedicineWithOCR", e)
            ChatSessionResult.Error("Σφάλμα: ${e.message}")
        }
    }

    private fun findBestMatch(
        medicines: List<MedRecord>,
        parsed: GeminiOCRParser.ParsedMedicine
    ): MedRecord? {
        if (medicines.isEmpty()) return null

        Log.d(TAG, "🎯 Scoring ${medicines.size} medicines:")

        val scored = medicines.map { medicine ->
            var score = 0

            val medicineName = (medicine.brand ?: medicine.generic ?: "").lowercase()
            val parsedNameLower = parsed.medicineName.lowercase()

            // Score 1: Name similarity (most important - 100 points)
            if (medicineName.contains(parsedNameLower)) {
                score += 100
                Log.d(TAG, "  ✓ Name match: ${medicine.brand ?: medicine.generic}")
            } else if (parsedNameLower.contains(medicineName)) {
                score += 80
            }

            // Score 2: Dosage match (critical for accuracy - 50 points)
            if (parsed.dosage != null) {
                val medicineStr = "${medicine.brand} ${medicine.generic} ${medicine.strength}".lowercase()
                val dosageLower = parsed.dosage.lowercase()

                if (medicineStr.contains(dosageLower)) {
                    score += 50
                    Log.d(TAG, "  ✓ Dosage match: ${parsed.dosage}")
                } else {
                    val parsedNumbers = parsed.dosage.filter { it.isDigit() || it == '/' }
                    val medicineNumbers = medicine.strength?.filter { it.isDigit() || it == '/' } ?: ""
                    if (parsedNumbers.isNotEmpty() && medicineNumbers.contains(parsedNumbers)) {
                        score += 40
                        Log.d(TAG, "  ✓ Partial dosage match: $parsedNumbers")
                    }
                }
            }

            // Score 3: Active substance match (helpful confirmation - 30 points)
            if (parsed.activeSubstance != null) {
                val activeInMedicine = medicine.generic?.lowercase() ?: ""
                val activeParsed = parsed.activeSubstance.lowercase()

                if (activeInMedicine.contains(activeParsed) || activeParsed.contains(activeInMedicine)) {
                    score += 30
                    Log.d(TAG, "  ✓ Active substance match")
                }
            }

            Log.d(TAG, "  ${medicine.brand ?: medicine.generic} → Score: $score")
            medicine to score
        }

        return scored
            .filter { it.second >= 100 }
            .maxByOrNull { it.second }
            ?.first
            .also { best ->
                if (best != null) {
                    Log.d(TAG, "✅ Best match: ${best.brand ?: best.generic}")
                } else {
                    Log.d(TAG, "⚠️ No medicine scored above threshold (need >= 100 points)")
                }
            }
    }

    private suspend fun continueWithMedicines(
        medicines: List<MedRecord>,
        medQuery: String,
        ocrText: String,
        parsed: GeminiOCRParser.ParsedMedicine?
    ): ChatSessionResult {
        val chosen = medicines.first()

        val context = buildMap<String, Any?> {
            put("language", "el")
            put("intent", "dosage_and_admin")
            put("ocrText", ocrText)
            put(
                "parsed",
                mapOf(
                    "brand" to (parsed?.medicineName ?: ""),
                    "dosage" to (parsed?.dosage ?: ""),
                    "active" to (parsed?.activeSubstance ?: "")
                )
            )
            put(
                "medicine",
                mapOf(
                    "id" to (chosen.id ?: ""),
                    "brand" to (chosen.brand ?: ""),
                    "generic" to (chosen.generic ?: ""),
                    "strength" to (chosen.strength ?: ""),
                    "form" to (chosen.form ?: ""),
                    "url" to (chosen.url ?: "")
                )
            )
        }

        Log.d(TAG, "Starting chat with medicine: ${chosen.brand ?: chosen.generic ?: medQuery}")

        val chatResponse = ApiClient.chat.chat(
            ChatRequest(
                sessionId = null,
                text = "Δείξε μου τη δοσολογία και οδηγίες χορήγησης για αυτό το φάρμακο.",
                medQuery = chosen.brand ?: chosen.generic ?: medQuery,
                context = context
            )
        )

        Log.d(TAG, "Chat started successfully: ${chatResponse.sessionId}")

        return ChatSessionResult.Success(
            sessionId = chatResponse.sessionId,
            reply = chatResponse.reply,
            extractedText = ocrText,
            medicines = listOf(chosen),
            sources = extractSources(chatResponse.metadata)
        )
    }

    suspend fun sendMessage(
        sessionId: String,
        message: String
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message in session $sessionId: $message")

            val trimmed = message.trim()
            val intent =
                if (trimmed.equals("δοσολογία", ignoreCase = true) ||
                    trimmed.contains("dose", ignoreCase = true) ||
                    trimmed.contains("dosage", ignoreCase = true) ||
                    trimmed.contains("posology", ignoreCase = true) ||
                    trimmed.contains("δοσολογ", ignoreCase = true)
                ) {
                    "dosage_and_admin"
                } else if (trimmed.contains("ανεπιθύμητ", ignoreCase = true) ||
                    trimmed.contains("παρενέργ", ignoreCase = true) ||
                    trimmed.contains("side effect", ignoreCase = true)
                ) {
                    "adverse_effects"
                } else if (trimmed.contains("αντενδείξ", ignoreCase = true) ||
                    trimmed.contains("contraindicat", ignoreCase = true)
                ) {
                    "contraindications"
                } else {
                    "general"
                }

            val response = ApiClient.chat.chat(
                ChatRequest(
                    sessionId = sessionId,
                    text = message,
                    context = mapOf(
                        "language" to "el",
                        "intent" to intent
                    )
                )
            )

            Log.d(TAG, "Response received: ${response.reply.take(80)}...")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    suspend fun startChatWithMedicine(
        medicineName: String,
        userQuestion: String = "Πες μου για αυτό το φάρμακο"
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting new chat with medicine: $medicineName")

            val response = ApiClient.chat.chat(
                ChatRequest(
                    sessionId = null,
                    text = userQuestion,
                    medQuery = medicineName,
                    context = mapOf("language" to "el")
                )
            )

            Log.d(TAG, "Chat started: ${response.sessionId}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting chat", e)
            Result.failure(e)
        }
    }

    suspend fun searchMedicines(
        query: String,
        language: String = "el",
        source: String = "galinos"
    ): Result<List<MedRecord>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching medicines: $query")

            val response = ApiClient.med.search(
                q = query,
                lang = language,
                source = source
            )

            Log.d(TAG, "Found ${response.results.size} results")
            Result.success(response.results)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching medicines", e)
            Result.failure(e)
        }
    }

    private fun cleanMedicineName(rawText: String): String {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return rawText.trim()

        val skipPrefixes = setOf(
            "gsk", "pfizer", "bayer", "novartis", "roche", "merck",
            "lilly", "sanofi", "astrazeneca", "boehringer", "abbott",
            "takeda", "amgen", "glaxo", "smithkline"
        )

        for (line in lines) {
            val lower = line.lowercase()

            if (line.all { it.isDigit() || it in setOf(' ', '-') }) continue
            if (line.length < 3) continue
            if (skipPrefixes.any { lower.startsWith(it) }) continue

            if (lower.contains("επικαλυ") || lower.contains("δισκί") ||
                lower.contains("καψάκ") || lower.contains("σιρόπ")
            ) continue

            var cleaned = line
                .replace(
                    Regex("""\s+\d+(/\d+)?\s*(mg|ml|g|mcg).*""", RegexOption.IGNORE_CASE),
                    ""
                )
                .trim()

            if (cleaned.length < 3) cleaned = line
            return cleaned
        }

        return lines.firstOrNull { it.length >= 5 } ?: lines.firstOrNull() ?: rawText
    }

    private fun extractSources(metadata: Map<String, Any?>?): List<ChatSource> {
        val sourcesList = metadata?.get("sources") as? List<*> ?: return emptyList()

        return sourcesList.mapNotNull { source ->
            if (source is Map<*, *>) {
                val title = source["title"] as? String ?: return@mapNotNull null
                val url = source["url"] as? String ?: return@mapNotNull null
                ChatSource(title = title, url = url)
            } else null
        }
    }
}

sealed class ChatSessionResult {
    data class Success(
        val sessionId: String,
        val reply: String,
        val extractedText: String,
        val medicines: List<MedRecord>,
        val sources: List<ChatSource>
    ) : ChatSessionResult()

    data class Error(val message: String) : ChatSessionResult()
}

/**
 * Candidate builder helpers (top-level)
 */
private fun buildCandidatesForSearch(
    ocrText: String,
    parsed: GeminiOCRParser.ParsedMedicine?,
    preferredName: String?
): List<String> {
    val out = linkedSetOf<String>()

    val brand = parsed?.medicineName?.takeIf { it.isNotBlank() } ?: preferredName?.trim()
    val dosage = extractDosage(ocrText)

    if (!brand.isNullOrBlank()) {
        out += brand
        if (!dosage.isNullOrBlank()) out += "$brand $dosage"
    }

    parsed?.activeSubstance?.let { gen ->
        val g = gen.trim()
        if (g.isNotEmpty()) {
            out += g
            out += g.stripAccentsAndGreekFinalSigma()
            out += g.stripAccentsAndGreekFinalSigma().lowercase()

            if ('/' in g) {
                val parts = g.split('/').map { it.trim() }.filter { it.length >= 3 }
                out.addAll(parts)
                out += parts.joinToString(" ")
                out += parts.joinToString(" ") { it.stripAccentsAndGreekFinalSigma().lowercase() }
            }
        }
    }

    ocrText.lineSequence().map { it.trim() }.firstOrNull { it.length >= 3 }?.let { out += it }

    val lower = ocrText.lowercase()
    if (lower.contains("αμοξικ") && lower.contains("κλαβουλ")) {
        out += "Augmentin"
        if (!dosage.isNullOrBlank()) out += "Augmentin $dosage"
    }

    return out.map { it.trim() }.filter { it.isNotEmpty() }.map { it.take(80) }.distinct()
}

private fun extractDosage(text: String): String? {
    val rx = Regex(
        """\b\d+(?:[.,]\d+)?(?:/\d+(?:[.,]\d+)?)?\s?(?:mg|mcg|μg|g|mg/ml|mcg/ml|μg/ml|g/ml|mg/5 ml|g/5 ml)\b""",
        RegexOption.IGNORE_CASE
    )
    return rx.find(text)?.value?.replace(',', '.')
}

private fun String.stripAccentsAndGreekFinalSigma(): String {
    val nfd = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    val noMarks = nfd.replace(Regex("\\p{Mn}+"), "")
    return noMarks.replace('ς', 'σ').replace(Regex("\\s+"), " ").trim()
}
