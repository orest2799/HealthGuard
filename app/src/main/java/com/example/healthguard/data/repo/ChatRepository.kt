package com.example.healthguard.data.repo

import android.util.Log
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

class ChatRepository {

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

                val extractedText = ocrResult.text ?: ocrResult.fullText ?: ""
                Log.d(TAG, "OCR extracted: $extractedText")

                if (extractedText.isBlank()) {
                    return@withContext ChatSessionResult.Error("Δεν βρέθηκε κείμενο στην εικόνα")
                }

                // Step 2: Clean and search for medicine
                val cleanedName = cleanMedicineName(extractedText)
                Log.d(TAG, "Step 2: Searching for medicine: $cleanedName")

                val searchResponse = ApiClient.med.search(
                    q = cleanedName,
                    lang = "el",
                    source = "galinos"
                )

                val medicines = searchResponse.results
                Log.d(TAG, "Found ${medicines.size} medicines")

                // Step 3: Start chat with medicine context
                val medQuery = medicines.firstOrNull()?.brand
                    ?: medicines.firstOrNull()?.generic
                    ?: cleanedName

                Log.d(TAG, "Step 3: Starting chat with medicine: $medQuery")

                val chatResponse = ApiClient.chat.chat(
                    ChatRequest(
                        sessionId = null,  // New session
                        text = "Δείξε μου πληροφορίες για αυτό το φάρμακο",
                        medQuery = medQuery,
                        context = mapOf(
                            "ocrText" to extractedText,
                            "language" to "el"
                        )
                    )
                )

                Log.d(TAG, "Chat started successfully: ${chatResponse.sessionId}")

                ChatSessionResult.Success(
                    sessionId = chatResponse.sessionId,
                    reply = chatResponse.reply,
                    extractedText = extractedText,
                    medicines = medicines,
                    sources = extractSources(chatResponse.metadata)
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error in processMedicineImage", e)
                ChatSessionResult.Error("Σφάλμα: ${e.message}")
            }
        }

    /**
     * Continue an existing chat session
     */
    suspend fun sendMessage(
        sessionId: String,
        message: String
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message in session $sessionId: $message")

            val response = ApiClient.chat.chat(
                ChatRequest(
                    sessionId = sessionId,
                    text = message
                    // No medQuery or context needed for follow-up messages
                )
            )

            Log.d(TAG, "Response received: ${response.reply.take(50)}...")
            Result.success(response)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    /**
     * Start a new chat with specific medicine name (without OCR)
     */
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

    /**
     * Search for medicines (useful for autocomplete/suggestions)
     */
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

    /**
     * Clean medicine name from OCR text
     * Removes dosages, forms, and extra text
     */
    private fun cleanMedicineName(rawText: String): String {
        // Take first line which usually contains the medicine name
        val firstLine = rawText.lines().firstOrNull()?.trim() ?: rawText

        // Remove common patterns that aren't part of the medicine name
        var cleaned = firstLine
            // Remove dosages
            .replace(Regex("\\d+\\s*mg.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+\\s*ml.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+\\s*g.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+\\s*mcg.*", RegexOption.IGNORE_CASE), "")
            // Remove forms (English)
            .replace(Regex("tablets?.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("caps.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("syrup.*", RegexOption.IGNORE_CASE), "")
            // Remove forms (Greek)
            .replace(Regex("δισκία.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("καψάκια.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("σιρόπι.*", RegexOption.IGNORE_CASE), "")
            .trim()

        // Take first 2-3 words (usually the medicine name)
        val words = cleaned.split("\\s+".toRegex())
        cleaned = words.take(3).joinToString(" ")

        Log.d(TAG, "Cleaned medicine name: '$rawText' -> '$cleaned'")
        return cleaned
    }

    /**
     * Extract sources from metadata
     */
    private fun extractSources(metadata: Map<String, Any?>?): List<ChatSource> {
        val sourcesList = metadata?.get("sources") as? List<*> ?: return emptyList()

        return sourcesList.mapNotNull { source ->
            if (source is Map<*, *>) {
                val title = source["title"] as? String ?: return@mapNotNull null
                val url = source["url"] as? String ?: return@mapNotNull null
                ChatSource(title = title, url = url)
            } else {
                null
            }
        }
    }
}

/**
 * Result wrapper for the complete image processing flow
 */
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