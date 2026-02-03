package com.example.healthguard.data.repo

import android.util.Log
import com.example.healthguard.data.models.MedicineOcrResult
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.ChatRequest
import com.example.healthguard.data.network.dto.ChatResponse
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.data.network.dto.MedRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatRepository {

    companion object {
        private const val TAG = "ChatRepository"
    }


    suspend fun processMedicineImage(imageFile: File): ChatSessionResult =
        withContext(Dispatchers.IO) {
            try {
                // Χρήση ασφαλούς RequestBody
                val requestBody = imageFile.asRequestBody("image/jpeg".toMediaType())

                // 1. Κλήση OCR
                val ocrResult = ApiClient.vision.scanMedicine(requestBody)

                // 2. Έλεγχος αν αναγνωρίστηκε η μάρκα (brand)
                if (ocrResult.brand.isNullOrBlank()) {
                    return@withContext ChatSessionResult.Error("Η αναγνώριση απέτυχε. Δοκίμασε πιο καθαρή φωτογραφία.")
                }

                // 3. Μετάβαση στη δημιουργία Chat
                continueWithStructuredData(ocrResult)

            } catch (e: Exception) {
                Log.e(TAG, "OCR Error: ${e.message}")
                ChatSessionResult.Error("Σφάλμα κατά την ανάλυση της εικόνας: ${e.localizedMessage}")
            }
        }

    private suspend fun continueWithStructuredData(ocrResult: MedicineOcrResult): ChatSessionResult {
        val allResults = mutableListOf<MedRecord>()


        val defaultRecord = MedRecord(
            brand = ocrResult.brand ?: "Άγνωστο Φάρμακο",
            summary = "Δραστική: ${ocrResult.activeSubstance ?: "-"}, Ισχύς: ${ocrResult.strength ?: "-"}",
            source = "Gemini OCR"
        )
        allResults.add(defaultRecord)

        val chatContext = mapOf(
            "brand" to (ocrResult.brand ?: ""),
            "activeSubstance" to (ocrResult.activeSubstance ?: ""),
            "strength" to (ocrResult.strength ?: ""),
            "form" to (ocrResult.form ?: "")
        )

        return try {
            val chatResponse = ApiClient.chat.chat(
                ChatRequest(
                    sessionId = null,
                    text = "Γεια! Δείξε μου πληροφορίες για το ${ocrResult.brand ?: "φάρμακο"}.",
                    medQuery = ocrResult.brand,
                    context = chatContext
                )
            )

            return ChatSessionResult.Success(
                sessionId = chatResponse.sessionId ?: "",
                reply = chatResponse.reply ?: "...",
                extractedText = ocrResult.brand ?: "",
                medicines = allResults,
                sources = extractSources(chatResponse.metadata),
                ocrResult = ocrResult // ΠΕΡΑΣΕ ΤΟ ΕΔΩ
            )
        } catch (e: Exception) {
            Log.e(TAG, "Chat initialization failed", e)
            ChatSessionResult.Error("Αποτυχία εκκίνησης Chat: ${e.localizedMessage}")
        }
    }

    suspend fun sendMessage(
        sessionId: String,
        message: String,
        medicineContext: String? = null,
        activeSubstance: String? = null,
        strength: String? = null, // ΠΡΟΣΘΗΚΗ
        form: String? = null      // ΠΡΟΣΘΗΚΗ

    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.chat.chat(
                ChatRequest(
                    sessionId = sessionId,
                    text = message,
                    context = mapOf(
                        "brand" to (medicineContext ?: ""),
                        "activeSubstance" to (activeSubstance ?: ""),
                        "strength" to (strength ?: ""),
                        "form" to (form ?: ""),
                        "language" to "el"
                    )
                )
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "SendMessage failed", e)
            Result.failure(e)
        }
    }

    suspend fun startChatWithMedicine(
        ocrResult: MedicineOcrResult,
        medicineName: String,
        userQuestion: String
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.chat.chat(
                ChatRequest(
                    sessionId = null,
                    text = userQuestion,
                    medQuery = medicineName,
                    context = mapOf(
                        "brand" to (ocrResult.brand ?: ""),
                        "activeSubstance" to (ocrResult.activeSubstance ?: ""),
                        "strength" to (ocrResult.strength ?: ""),
                        "form" to (ocrResult.form ?: ""),
                        "language" to "el"
                    )
                )
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractSources(metadata: Map<String, Any?>?): List<ChatSource> {
        val sources = mutableListOf<ChatSource>()
        try {
            val rawSources = metadata?.get("sources") as? List<*> ?: return emptyList()
            for (item in rawSources) {
                if (item is Map<*, *>) {
                    val title = item["title"]?.toString()
                    val url = item["url"]?.toString()
                    if (!title.isNullOrBlank() && !url.isNullOrBlank()) {
                        sources.add(ChatSource(title, url))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Source Parsing Error", e)
        }
        return sources
    }
}

sealed class ChatSessionResult {
    data class Success(
        val sessionId: String,
        val reply: String,
        val extractedText: String,
        val medicines: List<MedRecord>,
        val sources: List<ChatSource>,
        val ocrResult: MedicineOcrResult
    ) : ChatSessionResult()
    data class Error(val message: String) : ChatSessionResult()
}